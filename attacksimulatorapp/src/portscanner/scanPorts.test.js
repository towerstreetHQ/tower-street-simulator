import { scanAddressPort } from './scanPorts'

it('checks port 80 on www.ceai.io', () => {
    expect.assertions(1)
    return scanAddressPort('104.25.150.6', 80)
        .then(outcome => expect(outcome).toBe(true))    
})

it('checks port 81 on 128.23.0.1', () => {
    expect.assertions(1)
    return scanAddressPort('128.23.0.1', 81)
        .then(outcome => expect(outcome).toBe(false))
})
