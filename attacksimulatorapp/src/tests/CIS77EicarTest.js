import Test from '../framework/Test';
import TestResult from '../framework/TestResult';
import ServerRequest from '../framework/ServerRequest';


const testRunner = 'CIS77EicarTest'

export default class CIS77EicarTest extends Test {

    async run() {
        if (this.checkParameters('string', 'protocol', 'filename')) {
            return new TestResult(this.parameters.testId, testRunner, false, "Missing 'url' parameter.")
        } else {
            const fileServer = this.parameters.protocol === "https" ? 
                process.env.REACT_APP_HTTPS_FILE_SERVER : process.env.REACT_APP_FILE_SERVER

            const downloadUrl = `${this.parameters.protocol}://${fileServer}/test/resources/eicar/${this.parameters.filename}`;

            return new ServerRequest(downloadUrl)
                .loggingEnabled()
                .awaitBinaryResponse()
                .get()
                .then(response => { 
                    console.log(response)
                    return new TestResult(this.parameters.testId, testRunner, true,"Successfully downloaded eicar file",
                        {downloadUrl: downloadUrl})
                })
                .catch(reason => {
                    return new TestResult(
                        this.parameters.testId,
                        testRunner,
                        false,
                        "Failed downloading eicar file",
                        Object.assign(this.parameters, reason, {downloadUrl: downloadUrl})
                    )
                })
        }
    }

}
