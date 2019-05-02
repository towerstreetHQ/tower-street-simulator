import TestResult from './TestResult';

/**
 * Parent Test class which should contain some helper methods (not all like network requests, etc).
 * The run() method must be re-implemented by all child classes and must return an instance of TestResult class.
 */
class Test {

    constructor(parameters, outcomeToken) {
        this.parameters = parameters;
        this.outcomeToken = outcomeToken;
    }

    run() {
        return new TestResult("fooBarId", "FooBar", false, "Unimplemented tests executed.");
    }

    /** Returns true if this.parameters contains values of a provided type for all requested keys */
    checkParameters(tpe, ...names) {
        return names.forEach(name => {
            return this.parameters[name] !== undefined && typeof(this.parameters[name]) === tpe
        })
    }

}

export default Test;