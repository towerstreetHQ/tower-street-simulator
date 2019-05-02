import Test from '../framework/Test';
import TestResult from '../framework/TestResult';

const testRunner = 'CIS77WannaCryTest'

export default class CIS77WannaCryTest extends Test {

    async run() {
        if (this.checkParameters('string', 'first', 'second')) {
            return new TestResult(this.parameters.testId, testRunner, false, "Missing 'first' or 'second' parameter.")
        } else {
            return await new Promise(resolve => {
                const url = `http://www.${this.parameters.first}${this.parameters.second}.com/`
                console.log("Connecting to ", url)
                const s = document.createElement("script")
                s.addEventListener('load', () => {
                    s.innerText = ''
                    resolve(new TestResult(this.parameters.testId, testRunner, true, `Successful connect to WannaCry domain.`, 
                        { first: this.parameters.first, second: this.parameters.second }))
                })
                s.addEventListener('error', () => {
                    resolve(new TestResult(this.parameters.testId, testRunner, false, `Failed connecting to WannaCry domain.`, 
                        { first: this.parameters.first, second: this.parameters.second }))
                })
                s.src = url
                document.head.appendChild(s)
            })
        }
    }

}
