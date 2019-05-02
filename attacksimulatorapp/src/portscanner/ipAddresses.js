import { scanAddressPort } from './scanPorts'

/** Converts IP address from string to unsigned int */
export function fuIPAddress(sIPAddress) {
    var asComponents = /([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3})/.exec(sIPAddress)
    if (!asComponents) throw new TypeError("Invalid IPv4 address " + sIPAddress)
    var uIPAddress = 0
    for (var uByte = 0; uByte < 4; uByte++) {
      uIPAddress = (uIPAddress << 8) + parseInt(asComponents[uByte + 1], 10); // no sanity checks!
    }
    return uIPAddress;
}

/** Converts IP address from unsigned int to string */
export function fsIPAddress(uIPAddress) {
    var asIPAddress = []
    for (var uByte = 0; uByte < 4; uByte++) {
      asIPAddress[uByte] = ((uIPAddress >> (24 - uByte * 8)) & 0xFF).toString()
    }
    return asIPAddress.join(".")
}

function getRTCPeerConnection(rtcWindow) {
    return rtcWindow && (rtcWindow.RTCPeerConnection || rtcWindow.mozRTCPeerConnection || rtcWindow.webkitRTCPeerConnection)
}

/**
 * Checks if string contains a valid IPv4 or IPv6 address
 * 
 * @param {string} ipAddr value to check
 * @returns true if ipAddr contains valid IP address string
 */
export function isAnIPAddress(ipAddr) {
    return (/[0-9]{1,3}(?:\.[0-9]{1,3}){3}|[a-f0-9]{1,4}(?::[a-f0-9]{1,4}){7}/.exec(ipAddr) !== null)
}

/** 
 * Determines IP addresses associated to a client 
 * 
 * @param {element} iframe of a hidden <iframe>
 * @return Array of IP adresses
 */
export default async function ipAddresses(iframe) {
    return await new Promise((resolve, reject) => {
        const cRTCPeerConnection = getRTCPeerConnection(window) || getRTCPeerConnection(iframe.contentWindow)
        if (!cRTCPeerConnection) {
            reject('RTCPeerConnection is unavailable. Unable to determine browser IP address.')
        }

        try { 
            var oRTCPeerConnection = new cRTCPeerConnection(
                // Plan B is used because of change in chrome API in version 72 - https://www.chromestatus.com/feature/5723303167655936
                // The WebRTC specification is still evolving. There are two SDP formats on Chrome: the SDP format (Unified Plan) defined 
                // by the specification and the SDP format ( Plan B ) defined by Chrome. Firefox supports the former. If you do not specify 
                // the SDP format, Chrome72 uses the Unified Plan by default. 
                { iceServers: [], sdpSemantics: 'plan-b' },
                { optional: [{ RtpDataChannels: true }]}
            )

            if (oRTCPeerConnection.createDataChannel) {
                rtcIceCandidateIpResolver(oRTCPeerConnection, resolve, reject)
            } else if (window.RTCIceGatherer) {
                rtcIceGathererIpResolver(resolve, reject)
            } else {
                reject('RTC ICE is unavailable. Unable to determine browser IP address.')
            }
        }
        catch (e) { 
            reject("IP resolver crashed. Unable to determine browser IP address.", e) 
        }
    })
}

function rtcIceCandidateIpResolver(oRTCPeerConnection, resolve, reject) {
    var discoveredIPAddresses = new Set()
    oRTCPeerConnection.onicecandidate = (iceEvenet) => {
        const iceCandidate = iceEvenet.candidate
        if (iceCandidate) {
            const asCandidate = iceCandidate.candidate.split(' ')
            if (asCandidate[7] === 'host') {
                const sourceIPAddress = asCandidate[4]
                if (isAnIPAddress(sourceIPAddress)) { discoveredIPAddresses.add(sourceIPAddress) }
                else { console.log('Ignored RTCIceCandidate - not an IP address', iceCandidate.candidate) }
            } else { 
                console.log('Ignored RTCIceCandidate - not a host', iceCandidate.candidate)
            }                
        } else {
            resolve(Array.from(discoveredIPAddresses.keys()))
        }
    }

    oRTCPeerConnection.createDataChannel("")
    oRTCPeerConnection.createOffer()
        .then(sessionDescription => {
            oRTCPeerConnection.setLocalDescription(sessionDescription)
                .then(result => { console.log("Detecting IP addresses!") })
                .catch(reason => reject("Could not create offer", reason))
        })
        .catch(reason => reject("Could not create offer", reason))
}

// Resolving IP for EDGE browser
function rtcIceGathererIpResolver(resolve, reject) {
    var oRTCIceGatherer = new window.RTCIceGatherer({
        gatherPolicy: "all",
        iceServers: [],
    })

    var discoveredIPAddresses = new Set()
    oRTCIceGatherer.onlocalcandidate = (iceEvenet) => {
        if (iceEvenet.candidate.type === "host") {
            discoveredIPAddresses.add(iceEvenet.candidate.ip)
        } else if (iceEvenet.candidate.type) {
            console.log('Ignored RTCIceCandidate - not a host', iceEvenet.candidate)
        } else {
            resolve(Array.from(discoveredIPAddresses.keys()))
        }
    }
    oRTCIceGatherer.onerror = (reason) => {
        console.log("error", reason)
        reject("Could not create offer", reason)
    }
    console.log("Detecting IP addresses!")
}

/**
 * Determines length of network subnet by broadcast detection
 * @param {string} ipAddress 
 */
export async function getNetworkSubnetPrefix(ipAddress) {

    // Attempting to make an XHR to the broadcast address will result in an immediate error. Attempting to make an
    // XHR to an unused IP address will result in a time-out. We'll start with a large prefix and try increasingly
    // smaller ones to look for potential broadcast addresses using this timing difference. An IP address can also
    // be in-use by a *nix machine, which will also result in an immediate error. In an attempt to distinguish
    // between these two, try the next smaller prefix as well: if that fails, assume the former prefix is right and
    // return. Obviously this is not perfect, but it seems to work well enough.
    async function testPrefix(prefixLength, suspectedBroadcastFound) {
        return new Promise((resolve, reject) => {
            if (prefixLength >= 16) {
                const ones = (1 << (32 - prefixLength)) - 1;
                const broadcastIPAddress = fsIPAddress(ipAddress | ones);
                
                scanAddressPort(broadcastIPAddress, 2)
                    .then(success => {
                        if (success) {
                            // This IP address results in an immediate error. It may be the broadcast address.
                            if (prefixLength === 16) {
                                // We won't try to scan larger networks: use this.
                                resolve(prefixLength)
                            } else {
                                // Try the next: in most setups this should fail if we just found the broadcast address.
                                testPrefix(prefixLength - 1, true)
                                    .then(success => resolve(success))
                                    .catch(reason => reject(reason))
                            }
                        } else {
                            if (!suspectedBroadcastFound) {
                                // This IP address is not used, nor was the previously tested one: try the next.
                                testPrefix(prefixLength - 1, suspectedBroadcastFound)
                                    .then(success => resolve(success))
                                    .catch(reason => reject(reason))
                            } else {
                                // This IP address is not used, so the previous one is probably the broadcast address.
                                resolve(prefixLength + 1)
                            }
                        }
                    })
            } else {
                reject("Could not determine subnet shorter than /16")
            }
        })
    }

    return testPrefix(26, false);
    
}