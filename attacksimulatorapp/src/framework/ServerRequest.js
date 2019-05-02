import fetch from 'cross-fetch'
import encoding from 'text-encoding';
import RequestError, { RequestErrorTypes, resolveErrorType } from './RequestError'
import RequestResult from './RequestResult'

const BINARY_HEADER = {'Content-Type': 'application/octet-stream'}
const JSON_HEADER = {'Content-Type': 'application/json; charset=utf-8'}
const PLAINTEXT_HEADER = {'Content-Type': 'text/plain'}

const JSON_ERROR_FIELD = "message"

const RequestMethods = {
    GET: "GET",
    POST: "POST",
    DELETE: "DELETE",
    PUT: "PUT",
}

const ResponsePayloadType = {
    NO_CONTENT: "NO_CONTENT",
    JSON: "JSON",
    TEXT: "TEXT",
    BINARY: "BINARY",
    BINARY_OBFUSCATED: "BINARY_OBFUSCATED"
}

/**
 * Represents single request to the server. Holds URI, headers, payload and required response 
 * type to be used with HTTP requests. New instance could be provided with ServerRequestBuilder
 * class which is default for this package.
 * 
 * Request methods returns promise instance which will contain RequestResult instance on success
 * or throw RequestError on error. Request is successfull if response has code 2xx and contains 
 * required payload type. Otherwise one of error types is thrown.
 * 
 * Implementation is stateless. One instance could be used to provide multiple requests with 
 * the same parameters. 
 */
export class ServerRequest {
    _url = undefined
    _headers = { }
    _body = undefined
    _responsePayloadType = ResponsePayloadType.JSON
    _logging = false

    constructor(
        url, 
        headers = {}, 
        body = undefined, 
        responsePayloadType = ResponsePayloadType.JSON,
        logging = false
    ) {
        this._url = url
        this._headers = headers
        this._body = body
        this._responsePayloadType = responsePayloadType
        this._logging = logging
    }

    get() {
        return this._request(RequestMethods.GET)
    }

    post() {
        return this._request(RequestMethods.POST)
    }

    delete() {
        return this._request(RequestMethods.DELETE)
    }

    put() {
        return this._request(RequestMethods.PUT)
    }

    _request(method) {
        if (this._logging) {
            console.group("Sending "+ method +" request to server")
            console.info("Url: ", this._url.href)
            if (this._headers !== undefined) {
                console.info("Headers: ", this._headers)
            }
            if (this._body !== undefined) {
                console.info("Body: ", this._body)
            }
            if (this._responsePayloadType !== undefined) {
                console.info("Response payload type: ", this._responsePayloadType)
            }
            console.groupEnd()
        }
    
        return this._processResponse(
            fetch(this._url, {
                method: method,
                body: this._body,
                headers: this._headers
            })
        )
    }

    _processResponse = promise => {
        const req = promise
            // Throw network error if failed
            .catch(error => {throw new RequestError(RequestErrorTypes.NETWORK_ERROR, undefined, error.message)})
            // Process results
            .then(response => { 
                if (response.ok) {
                    // If status is ok then try to obtain response
                    return this._handleResponsePayload(response)
                } else {
                    // Throw error with resolved error message
                    return this._handleError(response)
                }
            })

        // Log returned value of enabled
        if (this._logging) {
            return req.then(
                response => {
                    console.group("Server request was successfully processed")
                    console.info("Url: ", this._url.href)
                    console.info("Response: ", response)
                    console.groupEnd()
                    return response
                },
                error => {
                    console.group("Error providing server request")
                    console.info("Url: ", this._url.href)
                    console.info("Error: ", error)
                    console.groupEnd()
                    throw error
                }
            )
        } else {
            return req
        }
    }

    /**
     * Helper method to extract required payload type from request body. 
     * 
     * To learn how to extract more payload types see:
     * https://github.github.io/fetch/#response-body
     */
    _handleResponsePayload(response) {
        switch (this._responsePayloadType) {
            
            case ResponsePayloadType.JSON:
                return this._handlePayload(response, response.json())

            case ResponsePayloadType.TEXT:
                return this._handlePayload(response, response.text())
            
            case ResponsePayloadType.BINARY:
                return this._handlePayload(response, response.blob())

            case ResponsePayloadType.BINARY_OBFUSCATED:
                return this._handlePayload(response, response.blob(), this._decodeObfuscated)

            // NO_CONTENT
            default:
                return new RequestResult(response.status)
        }
    }

    /**
     * Fetch library has data extractor methods which returns promise. We need to 
     * wait for it and return as RequestResult or throw error if not provided.
     */
    _handlePayload(response, payloadPromise, payloadF = (f) => { return new Promise(resolve => resolve(f)) }) {
        return payloadPromise.then(
            payload => { return payloadF(payload).then(pl => new RequestResult(response.status, pl)) },
            () => { throw new RequestError(RequestErrorTypes.MISSING_DATA, response.status) }
        )
    }

    _handleError(response) {
        const errorType = resolveErrorType(response.status)

        return response.json().then(
            json => {
                const msg = json[JSON_ERROR_FIELD]
                throw new RequestError(
                    errorType, 
                    response.status,
                    msg !== undefined ? msg : response.statusText)
            }, 
            () => {
                throw new RequestError(errorType, response.status, response.statusText)
            }
        )
    }

    _decodeObfuscated(blob) {
        return new Promise((resolve, reject) => {
            const fileReader = new FileReader()        
            fileReader.onload = event => {
                const arrayBuffer = event.target.result            
                const view = new DataView(arrayBuffer)
                for (var i = 0; i < view.byteLength; i++) view.setUint8(i, 0xFF ^ view.getUint8(i))
                const decoder = new encoding.TextDecoder('utf-8')
                resolve(decoder.decode(new Uint8Array(arrayBuffer)))    // We might want to wrap it back to BLOB using new Blob([new Uint8Array(data)]);
            }
            fileReader.readAsArrayBuffer(blob)            
        })
    }
}

/**
 * Builder class for ServerRequest class. Allows to specify request parameters in method chain.
 * New request instance can be provided by calling build method or using request methods. Request 
 * methods are shortcuts for this.build().get() and returns promise with server response.
 * 
 * Constructor takes one mandatory parameter: string representing query to the server (uri without 
 * parameters). 
 * 
 * One can use chain of methods to specify:
 *  - Headers
 *  - Query parameters
 *  - Request payload
 *  - Required response type (throws error if not provided by the server)
 *  - Enable logging
 * 
 * 
 * Usage:
 * 
 *  Import default builder transparently like this:
 *          import ServerRequest from '../framework/ServerRequest';
 *  
 *  Create new instance and use it:
 *          new ServerRequest("...").build()
 * 
 * 
 * Examples:
 * 
 *  1) Get request with api key as header
 *         new ServerRequest("http://127.0.0.1:5000/curve")
 *           .addHeader("X-Api-Key", "4cd1aa9b-22af-42ad-82d8-b5b8511fc8d5")
 *           .addQueryParameter("dist", 1)
 *           .addQueryParameter("min", 10)
 *           .addQueryParameter("med", 20)
 *           .addQueryParameter("max", 10)
 *           .loggingEnabled()
 *           .get()
 *           .then(response => {console.log(response)})
 * 
 * 
 *  2) Post with JSON payload
 *         new ServerRequest("http://127.0.0.1:5000/login")
 *           .addJsonPayload({
 *               username: "test",
 *               password: "testt"
 *           })
 *           .loggingEnabled()
 *           .post()
 *           .then(response => {console.log(response)})
 */
export default class ServerRequestBuilder {
    _query = undefined
    _headers = { }
    _queryParams = { }
    _body = undefined
    _payloadHeader = undefined
    _responsePayloadType = ResponsePayloadType.JSON
    _logging = false

    constructor(query) {
        this._query = query
    }

    addHeader(key, value) {
        this._headers[key] = value
        return this
    }

    addHeaders(headers) {
        Object.assign(this._headers, headers)
        return this
    }

    addQueryParameter(key, value) {
        this._queryParams[key] = value
        return this
    }

    addQueryParameters(queryParams) {
        Object.assign(this._queryParams, queryParams)
        return this
    }

    addBinaryPayload(binaryData){
        this._body = binaryData
        this._payloadHeader = BINARY_HEADER
        return this
    }

    addJsonPayload(jsonData) {
        this._body = JSON.stringify(jsonData)
        this._payloadHeader = JSON_HEADER
        return this
    }

    addPlaintextPayload(plaintext) {
        this._body = plaintext
        this._payloadHeader = PLAINTEXT_HEADER
        return this
    }

    awaitBinaryResponse() {
        this._responsePayloadType = ResponsePayloadType.BINARY
        return this
    }

    awaitBinaryObfuscatedResponse() {
        this._responsePayloadType = ResponsePayloadType.BINARY_OBFUSCATED
        return this
    }

    awaitNoContentResponse() {
        this._responsePayloadType = ResponsePayloadType.NO_CONTENT
        return this
    }

    awaitTextResponse() {
        this._responsePayloadType = ResponsePayloadType.TEXT
        return this
    }

    loggingEnabled(logging = true) {
        this._logging = logging
        return this
    }

    build() {
        const url = new URL(this._query)
        Object.keys(this._queryParams).forEach(key => {
            const p = this._queryParams[key]
            url.searchParams.append(key, p !== null ? p : "")
        })

        const headers = Object.assign({}, this._headers)
        if (this._payloadHeader !== undefined) {
            Object.assign(headers, this._payloadHeader)
        }

        return new ServerRequest(url, headers, this._body, this._responsePayloadType, this._logging)
    }

    get() {
        return this.build().get()
    }

    post() {
        return this.build().post()
    }

    delete() {
        return this.build().delete()
    }

    put() {
        return this.build().put()
    }
}