export default class Stopwatch {
    constructor() {
        this._start = this._now()
    }

    _now() {
        return performance.now()
    }

    duration() {
        return this._now() - this._start
    }
}