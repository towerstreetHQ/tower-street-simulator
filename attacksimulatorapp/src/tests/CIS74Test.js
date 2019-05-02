import Test from '../framework/Test';
import TestResult from '../framework/TestResult';

const testRunner = 'CIS74Test'
const defaultImagePath = 'favicon.ico'

/**
 * Tests ability to connect to arbitrary website by attempting to download favicon.ico.
 */
export default class CIS74Test extends Test {

    async run() {
        if (this.checkParameters('string', 'url')) {
            return new TestResult(this.parameters.testId, testRunner, false, "Missing 'url' parameter.")
        } else {
            return await new Promise(resolve => {
                const { url, imagePath, testId, timeout } = this.parameters
                const testUrl = this._resolveTestUrl(url, imagePath)

                const img = new Image();
                img.addEventListener('load', () => 
                    resolve(new TestResult(testId, testRunner, true, `Successful connect to website ${url}.`, { url, testUrl }))
                )
                img.addEventListener('error', () => 
                    resolve(new TestResult(testId, testRunner, false, `Failed connecting to website ${url}.`, { url, testUrl }))
                )
                img.src = testUrl;  
                
                if (timeout) {
                    setTimeout(function(){
                        resolve(new TestResult(testId, testRunner, false, `Timeouted connecting to website ${url} after ${timeout}ms.`, { url, testUrl }))
                    }, timeout);
                }
            });                
        }
    }

    _resolveTestUrl(url, imagePath = defaultImagePath) {
        // Shall we include "/" character between base and path, or it is already there?
        const pathSeparator = url.endsWith("/") || imagePath.startsWith("/") ? "" : "/"
        // Does path contain query parameters with "?" or we are free to use it to separate cache invalidate parameter?
        const dateSeparator = imagePath.includes("?") ? "&" : "?"

        return `${url}${pathSeparator}${imagePath}${dateSeparator}${Date.now()}`
    }

}