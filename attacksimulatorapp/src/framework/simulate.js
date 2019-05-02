import TestResult from './TestResult';
import Stopwatch from './Stopwatch';
import ServerRequest from '../framework/ServerRequest';

/**
 * Simulator function takes list of tests as a parameter and returns array of test results.
 */
export default async function simulate(tests, outcomeToken, testStartedCallback = testResult => {}) {

    if (tests instanceof Array) {
        const testResults = []
        for (var index = 0; index < tests.length; index++) {
            const result = await simulateSingle(tests[index], outcomeToken, testStartedCallback)
            testResults.push(result)
        }
        return testResults
    } else {
        console.error("Simulator's provided constructor parameter is not array");
        return [];
    }

}

async function simulateSingle(test, outcomeToken, testStartedCallback) {
    testStartedCallback(test)

    const sw = new Stopwatch()
    const result = await simulateTest(test, outcomeToken)
    result.duration = sw.duration()
    
    await sendResult(test, result, outcomeToken)

    return result
}

async function simulateTest(test, outcomeToken) {
    if (test.testId === undefined || test.testRunner === undefined || !(typeof(test.testId) === 'number') || !(typeof(test.testRunner) === 'string')) {
        return new TestResult(test.testId, test.testRunner, false, "Missing testId or testRunner value")
    } else {
        try {
            const testCls = require(`../tests/${test.testRunner}.js`).default;
            const result = (new testCls(test, outcomeToken)).run()

            if (result instanceof Promise) {
                return await result.catch(e => {
                    if (e instanceof TestResult) {
                        // Thrown exception is already result - return it
                        return e
                    } else {
                        // Unhandled test exception
                        return new TestResult(test.testId, test.testRunner, false, 
                            `Test failed in promise '${test.testRunner}': ${e.message}`, e);
                    }
                })
            } else {
                return result
            }
        } 
        catch(e) {
            console.error(e);
            return new TestResult(test.testId, test.testRunner, false, `Unsupported runner for testRunner '${test.testRunner}': ${e}`);
        }
    }
}

async function sendResult(test, result, outcomeToken) {
    if (outcomeToken !== undefined) {
        return await new ServerRequest(`${process.env.REACT_APP_SIMULATION_SERVER}/test/simulations/test-result?outcomeToken=${outcomeToken}&testId=${test.testId}`)
            .addJsonPayload(result)
            .awaitNoContentResponse()
            .post()
            .catch(sentFailed => {
                console.error('Failed send test result to server', sentFailed)
                throw sentFailed
            })
    }
}
