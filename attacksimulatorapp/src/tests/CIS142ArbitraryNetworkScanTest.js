import Test from '../framework/Test';
import {segmentedNetworkScanTest} from "./networkScanTest";
import ServerRequest from '../framework/ServerRequest';
import TestResult from '../framework/TestResult';

const testRunner = 'CIS142ArbitraryNetworkScanTest'

export default class CIS142ArbitraryNetworkScanTest extends Test {

    async run() {
        if (this.checkParameters('Array', 'subnets')) {
            return new TestResult(this.parameters.testId, testRunner, false, "Missing 'subnets' array parameter.")
        } else {
            const { portScanTimeout, ports, portScanDelay } = this.parameters

            const serverSubnets = await this.fetchSubnets()
            console.log("Retrieved discovered subnets", serverSubnets.payload)

            const subnets = this.parameters.subnets
                .concat(serverSubnets.payload)
                
            return segmentedNetworkScanTest(
                this.parameters.testId,
                testRunner,
                subnets,
                ports,
                portScanTimeout,
                portScanDelay
            )
        }
    }

    fetchSubnets() {
        const segmentsUri = `${process.env.REACT_APP_SIMULATION_SERVER}/test/network/segments`
            +`?outcomeToken=${this.outcomeToken}&testId=${this.parameters.testId}`;

        return new ServerRequest(segmentsUri)
            .get()
            .catch( reason => {
                throw new TestResult(this.parameters.testId,
                    testRunner,
                    false,
                    "Failed to fetch network segments from server",
                    reason
                )
            })
    }
}

