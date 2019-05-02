/**
 * Class containing result of a single test run.
 */
export default class TestResult {

    constructor(testId, testRunner, result, message, obj = {}) {
        this.testId = testId;
        this.testRunner = testRunner;
        this.result = result;
        this.message = message;
        this.obj = obj;
        this.duration = null;
    }

    get testResult() {
        return `Test ${this.testId}/${this.testRunner}: ${this.result} - ${this.message}`;
    }

    get object() {
        return {
            testId: this.testId,
            testRunner: this.testRunner,
            result: this.result,
            message: this.message,
            object: this.obj,
            duration: this.duration
        };
    }

}
