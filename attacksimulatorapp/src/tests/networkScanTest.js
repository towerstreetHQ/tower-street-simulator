import {getIpRange, scanMachines} from "../portscanner/scanNetwork";
import {fuIPAddress} from "../portscanner/ipAddresses";
import TestResult from "../framework/TestResult";

export default async function networkScanTest(testId, testRunner, ipAddress, networkPrefix, ports, portScanTimeout, portScanDelay) {
    const subnets = [{ip: ipAddress, prefix: networkPrefix}]
    return segmentedNetworkScanTest(testId, testRunner, subnets, ports, portScanTimeout, portScanDelay)
}

export function segmentedNetworkScanTest(testId, testRunner, subnets, ports, portScanTimeout, portScanDelay) {
    // Gets IPS to be tested - ensure that each IP is included only once
    const resolvedIps = new Set()
    subnets.forEach(subnet => {
        const range = getIpRange(fuIPAddress(subnet.ip), subnet.prefix)
        range.forEach(ip => resolvedIps.add(ip))
    })

    const resolvedIpsArr = [];
    resolvedIps.forEach(v => resolvedIpsArr.push(v));

    return scanMachines(resolvedIpsArr, ports, portScanTimeout, portScanDelay)
        .then(scanResults => {
            return new TestResult(testId,
                testRunner,
                true,
                "Successfully performed network port scan",
                scanResults
            )
        })
        .catch(reason => {
            return new TestResult(testId,
                testRunner,
                false,
                "Failed performing network port scan",
                reason
            )
        })
}