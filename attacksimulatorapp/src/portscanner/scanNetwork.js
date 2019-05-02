import { fsIPAddress } from "./ipAddresses";
import { range } from "lodash";
import { scanAddressPort, defaultPorts } from "./scanPorts";
import PromisePool from "es6-promise-pool"
import Stopwatch from "../framework/Stopwatch"

const scanConcurrency = 32
const scanIpRangeLimit = 255
const defaultPortScanDelay = 40000

// Contains time IP address was scanned for every scheduled IP address 
// Used to calculate delay between accessing IP again (there has to be 40s delay to reset routing tables 
// otherwise we've got instant response)
// This constant is global because this needs to be shared between test runs.
const scannedAddresses = { }

function scanMachineThread(ipAddress, port, portScanTimeout, runDelay) {
    return new Promise(resolve => {
        setTimeout(() => { 
            const sw = new Stopwatch()
            scanAddressPort(ipAddress, port, portScanTimeout)
                .then(opened => resolve({
                    ipAddress: ipAddress, 
                    opened: opened, 
                    port: port,
                    runDelay: runDelay,
                    duration: sw.duration()
                }))
        }, runDelay)
    })
}

export default async function scanNetwork(ipAddress, prefix, ports, portScanTimeout, portScanDelay) {
    if (typeof(ipAddress) !== 'number' || typeof(prefix) !== 'number' || !Array.isArray(ports)) {
        throw new Error('Invalid parameter types for scanNetwork(number, number, Array)')
    }

    const machines = getIpRange(ipAddress, prefix)
    return await scanMachines(machines, ports, portScanTimeout, portScanDelay)
}

export function getIpRange(ipAddress, prefix) {
    if (typeof(ipAddress) !== 'number' || typeof(prefix) !== 'number') {
    throw new Error('Invalid parameter types for getIpRange(number, number)')
    }

    const bitMask = - (1 << (32 - prefix))
    const allOnes = (1 << (32 - prefix)) - 1
    const startIP = ipAddress & bitMask + 1
    const endIP = ipAddress | allOnes

    const scanStop = Math.min(startIP + scanIpRangeLimit, endIP)

    return range(startIP, scanStop).map(ip => { return fsIPAddress(ip) })
}

export function scanMachines(machines, ports = defaultPorts, portScanTimeout, portScanDelay = defaultPortScanDelay) {        
    return new Promise((resolve, reject) => {
        // Target object for results. There is no problem with concurrency because
        // javascript should be single threaded (due to smarter javascripters)
        const detectedPorts = { }

        // Generator for promise iterator - promises will be invoked based on
        // pool concurrency settings
        const generatePromises = function * () {
            for (let portIndex = 0; portIndex < ports.length; portIndex++) {
                const port = ports[portIndex]
                for (let index = 0; index < machines.length; index++) {
                    const ip = machines[index]
                    const lastRunTime = scannedAddresses[ip]
                    const currentTime = performance.now()

                    var runDelay = 0
                    if (lastRunTime !== undefined) {
                        const timeDelta = currentTime - lastRunTime
                        if (timeDelta < portScanDelay) {
                            runDelay = portScanDelay - timeDelta
                        }
                    }

                    scannedAddresses[ip] = currentTime + runDelay

                    yield scanMachineThread(ip, port, portScanTimeout, runDelay)
                }
            }
        }

        // Start concurrent promises and put result to detectedPorts variable
        const pool = new PromisePool(generatePromises(), scanConcurrency)
        pool.addEventListener('fulfilled', function (event) {
            const res = event.data.result
            detectedPorts[res.ipAddress] = Object.assign({}, detectedPorts[res.ipAddress], 
                {
                    [res.port]: {
                        opened: res.opened,
                        duration: res.duration
                    }
                }
            ) 
        })

        pool.start()
            .then(() => resolve(detectedPorts))
            .catch(reason => reject(reason))
    })
}