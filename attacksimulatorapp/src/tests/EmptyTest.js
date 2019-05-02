import Test from '../framework/Test';
import TestResult from '../framework/TestResult';

class EmptyTest extends Test {

    async run() {
        return new TestResult(this.parameters.testId, "EmptyTest", true, "Empty test executed.");
    }

}

export default EmptyTest;