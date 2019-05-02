import Test from '../framework/Test';
import TestResult from '../framework/TestResult';
import ipAddresses, { fuIPAddress } from "../portscanner/ipAddresses";
import networkScanTest from "./networkScanTest";
import ServerRequest from '../framework/ServerRequest';
import { timeout } from 'promise-timeout';

const testRunner = 'CIS141LocalNetworkScanTest';
const ipDetectTimeout = 30 * 1000;

// IP ranges for local subnets from rfc1918 and rfc6598
const validIpRanges = [
    {start: fuIPAddress('10.0.0.0'), stop: fuIPAddress('10.255.255.255')},
    {start: fuIPAddress('172.16.0.0'), stop: fuIPAddress('172.31.255.255')},
    {start: fuIPAddress('192.168.0.0'), stop: fuIPAddress('192.168.255.255')},
    {start: fuIPAddress('100.64.0.0'), stop: fuIPAddress('100.127.255.255')},
]

export default class CIS141LocalNetworkScanTest extends Test {

    async run() {
        const { portScanTimeout, ports, portScanDelay } = this.parameters

        const ipAddress = await this.getIpAddresses()
        const networkPrefix = await this.getSubnetPrefix(ipAddress)

        console.log(`Detected IP: ${ipAddress}/${networkPrefix}`)

        if (this.testIpInValidRange(ipAddress)) {
            // Send discovered subnet to server
            await this.storeSubnet(ipAddress, networkPrefix)

            // Perform port scan
            return await networkScanTest(this.parameters.testId, testRunner, ipAddress, networkPrefix, 
                ports, portScanTimeout, portScanDelay)
        } else {
            // Public internet IP - skip scanning
            return new TestResult(this.parameters.testId, testRunner, false,
                "IP address is not in permited IP range", { ip: ipAddress }
            )
        }
    }

    async getIpAddresses() {
        const getError = reason => new TestResult(this.parameters.testId, testRunner, 
            false, "Failed determining client's IP address", reason
        )

        // Try obtain IP from RPC detector
        try {
            const detectedIPs = await timeout(ipAddresses(document.getElementById('hiddenIFrame')), ipDetectTimeout)
            return detectedIPs[0]
        } catch(err) {
            console.warn(err);
        }

        // If failed try to get IP using server XFF header
        const response = await new ServerRequest(`${process.env.REACT_APP_SIMULATION_SERVER}/client-info/x-forwarded-for`)
            .get()
            .catch(reason => { throw getError(reason) })

        // Try get 
        if (response.payload) {
            return response.payload
        } else {
            throw getError(null)
        }
    }

    getSubnetPrefix(ipAddress) {
        // Prefix detection is not stable, just use the block 24 for discovered IP
        return 24
        /*
        return getNetworkSubnetPrefix(fuIPAddress(ipAddress))
            .catch(reason => {
                throw new TestResult(this.parameters.testId,
                    testRunner,
                    false,
                    "Failed to determine client's network prefix",
                    reason
            )
        })
        */
    }

    /**
     * Checkes whether given IP address belongs to the local subnet and not to public internet. 
     * IP address must be within the range specified by rfc1918 or rfc6598.
     * 
     * There are only few companies which actually have a public ip range in use, so we would not 
     * expect to find endpoints directly accessible from the internet.
     */
    testIpInValidRange(ipAddress) {
        const intAddr = fuIPAddress(ipAddress)
        const validRange = validIpRanges.findIndex((range) => 
            range.start <= intAddr && intAddr <= range.stop
        )

        return validRange > -1
    }

    /**
     * Send discovered IP and prefix to server.
     *
     * Server error is handled and logged, simulation continues uninterrupted.
     */
    storeSubnet(ipAddress, netPrefix) {
        const segmentDiscoveredUri = `${process.env.REACT_APP_SIMULATION_SERVER}/test/network/segments/discovered`
            +`?outcomeToken=${this.outcomeToken}&testId=${this.parameters.testId}`;

        return new ServerRequest(segmentDiscoveredUri)
            .addJsonPayload({ip: ipAddress, prefix: netPrefix})
            .awaitNoContentResponse()
            .post()
            .then(response => console.log("Uploaded discovered network segment", response))
            .catch( e => console.error("Failed to upload discovered network segment", e))
    }
}
