export const RequestErrorTypes = {
    NETWORK_ERROR: "NETWORK_ERROR",
    NOT_FOUND: "NOT_FOUND",
    SERVER_ERROR: "SERVER_ERROR",
    BAD_REQUEST: "BAD_REQUEST",
    MISSING_DATA: "MISSING_DATA",
    UNAUTHORIZED: "UNAUTHORIZED",
    FORBIDDEN: "FORBIDDEN",
    NOT_ALLOWED: "NOT_ALLOWED",
    SERVICE_UNAVAILABLE: "SERVICE_UNAVAILABLE",
    GATEWAY_TIMEOUT: "GATEWAY_TIMEOUT",
    REDIRECT: "REDIRECT",
    UNKNOWN_ERROR: "UNKNOWN_ERROR",
}

const codeMapping = {
    401: RequestErrorTypes.UNAUTHORIZED,
    403: RequestErrorTypes.FORBIDDEN,
    404: RequestErrorTypes.NOT_FOUND,
    405: RequestErrorTypes.NOT_ALLOWED,
    503: RequestErrorTypes.SERVICE_UNAVAILABLE,
    504: RequestErrorTypes.GATEWAY_TIMEOUT,
}

export function resolveErrorType(status) {
    if (codeMapping[status] !== undefined) {
        return codeMapping[status]
    } else if (status >= 500) {
        return RequestErrorTypes.SERVER_ERROR
    } else if (status >= 400) {
        return RequestErrorTypes.BAD_REQUEST
    } else if (status >= 300) {
        return RequestErrorTypes.REDIRECT
    } else {
        return RequestErrorTypes.UNKNOWN_ERROR
    }
}

export default class RequestError {
    constructor(type, code, message = "") {
        this.type = type;
        this.code = code;
        this.message = message;
    }
}