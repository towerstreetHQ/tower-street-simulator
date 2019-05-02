import Test from '../framework/Test';
import TestResult from '../framework/TestResult';
import ServerRequest from '../framework/ServerRequest';

function ArrayBufferToString(buffer) {
    return BinaryToString(String.fromCharCode.apply(null, Array.prototype.slice.apply(new Uint8Array(buffer))));
}

function BinaryToString(binary) {
    var error;

    try {
        return decodeURIComponent(escape(binary));
    } catch (_error) {
        error = _error;
        if (error instanceof URIError) {
            return binary;
        } else {
            throw error;
        }
    }
}

class DownloadFileToLSTest extends Test {

    run() {
        let endpoint = 'http://localhost:3000/favicon.ico'
        new ServerRequest(endpoint)
            .awaitBinaryResponse()
            .loggingEnabled()
            .get()
            .then(response => {
                var reader = new FileReader();
                reader.addEventListener("loadend", () => {
                    console.log("Reader result", ArrayBufferToString(reader.result))
                    localStorage.setItem("favicon", ArrayBufferToString(reader.result))
                    new ServerRequest(endpoint)
                        .addBinaryPayload(localStorage.getItem("favicon"))
                        .loggingEnabled()
                        .post()
                        .then(response => {console.log(response)})  
                })
                reader.readAsArrayBuffer(response.payload)
            })
        return new TestResult(this.parameters.testId, "EmptyTest", true, "Empty test executed.")
    }

}

export default DownloadFileToLSTest;