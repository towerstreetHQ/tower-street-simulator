import Test from '../framework/Test';
import TestResult from '../framework/TestResult';
import Fingerprint2 from 'fingerprintjs2';
import { getBrowserData } from '../Browser'

const testRunner = 'EnvironmentTest'

function snakeToCamel(s){
    return s.replace(/(_\w)/g, function(m){return m[1].toUpperCase();});
}

export default class EnvironmentTest extends Test {
    async run() {
        const parameters = this.parameters

        return await new Promise(resolve => {
            new Fingerprint2().get(function(fingerprint, components) {
                // Detect aditional browser info from user agent parser + include calculated fingerprint
                const resultObj = {
                    fingerprint: fingerprint,
                    browserData: getBrowserData()
                }

                // Components are array of key value pairs - convert to object and also 
                // ensure camel case property names
                components.forEach(element => {
                    resultObj[snakeToCamel(element.key)] = element.value
                });

                resolve(new TestResult(parameters.testId, testRunner, true, `Browser capabilities.`, resultObj))
            })
        })
    }

}