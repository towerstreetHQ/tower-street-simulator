const defaultTimeout = 3000
export const defaultPorts = [80, 443, 445, 4000, 5900, 65534]

/**
 * Scans port of an IP address
 * 
 * @param {string} ipAddress 
 * @param {number} port 
 * @returns {boolean} true if port responsive
 */
export async function scanAddressPort(ipAddress, port, portScanTimeout = defaultTimeout) {
    return new Promise(resolve => {
        var xhr = new XMLHttpRequest()
        var aborted = false
        var to = setTimeout(() => {            
            if (!aborted) {
                aborted = true
                xhr.abort()
                resolve(false)
            }
        }, portScanTimeout)
        xhr.onreadystatechange = event => {            
            if (xhr.readyState === 4 && !aborted) {
                clearTimeout(to)
                resolve(true)
            }
        }
        xhr.open("GET", `http://${ipAddress}:${port}/favicon.ico?${Date.now()}`)
        xhr.send()
    })
}
