import { isAnIPAddress } from './ipAddresses'

it('checks valid IPv4 address', () => {
    expect(isAnIPAddress('127.0.0.1')).toBe(true)
})

it('check valid IPv6 address', () => {
    expect(isAnIPAddress('2001:0db8:85a3:0000:0000:8a2e:0370:7334')).toBe(true)
})

it('checks invalid IP address', () => {
    expect(isAnIPAddress('Hello world')).toBe(false)
})


// TODO determining IP address tests are not automated due to missing RTCPeerConnection support
/* it("determines client's IP addresses", async () => {    
    const iframe = document.createElement('iframe')
    const ipAddress = await ipAddresses(iframe)
    expect(typeof(ipAddress) === 'string').toBe(true)
    expect(ipAddress.length).toBeGreaterThanOrEqual(7)
}) */
