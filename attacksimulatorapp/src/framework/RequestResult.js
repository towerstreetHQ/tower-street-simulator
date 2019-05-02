export default class RequestResult {
    constructor(status, payload = undefined) {
        this.status = status;
        this.payload = payload;
    }
}