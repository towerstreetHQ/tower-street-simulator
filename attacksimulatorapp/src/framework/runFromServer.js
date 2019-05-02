import ServerRequest from './ServerRequest';
import simulate from './simulate';

import queryString from 'qs';
import uuidValidate from 'uuid-validate';

const nullUUID = '00000000-0000-0000-0000-00000000000'
const heartbeatFrequency = 5000
let heartbeatId = null

class RunException {
    constructor(brokenLink, message = "") {
        this.brokenLink = brokenLink;
        this.message = message;
    }
}

export default async function runFromServer(
    simulationToken,
    simulationStartedCallback = (simulationToken, outcomeToken, tests) => {}, 
    testStartedCallback = (test) => {}
) {
    // Obtain test definitions from server
    console.log(`Download configuration for simulation token ${simulationToken}`)
    const testDefinition = await getTestDefinitionFromServer(simulationToken)
    const outcomeToken = testDefinition.outcomeToken
    const tests = testDefinition.tests
    console.log(`Received list of tests for simulation ${simulationToken}`, testDefinition)

    // Tell caller that stest definition is obtained and simulation started
    simulationStartedCallback(simulationToken, outcomeToken, tests)

    // Provide simulation
    startHeartbeat(outcomeToken)
    const simOutcome = await simulate(tests, outcomeToken, testStartedCallback)
    console.log('Simulation outcome', simOutcome)
    stoptHeartbeat()

    // Upload test results
    const sent = await sendResultsToServer(simOutcome, outcomeToken)
    console.log('Successfully set results of simulations to server', sent)

    return sent
}

export async function getSimulationToken() {
    const queryParameters = queryString.parse(window.location.search.replace(/^\?/, ''))
    const simulationToken = queryParameters.simulationToken
    if (typeof simulationToken === 'string') {
        // UUID with all zeros needs to be validated separately, it is not valid type 1 - 5
        if (simulationToken === nullUUID || uuidValidate(simulationToken)) {
            return simulationToken
        } else {
            console.error(`Simulation token is not valid UUID: ${simulationToken}`)
            throw new RunException(true, 'Simulation token is not valid UUID')
        }
    } else {
        console.error(`Invalid or missing simulationToken search parameter: ${simulationToken}`)
        throw new RunException(true, 'Invalid or missing simulationToken search parameter')
    }
  }

async function getTestDefinitionFromServer(simulationToken) {
    return await new ServerRequest(`${process.env.REACT_APP_SIMULATION_SERVER}/test/simulations/start-simulation?simulationToken=${simulationToken}`)
        .post()
        .then(
            // Process response
            response => {
                if (response.status === 200 &&
                    typeof response.payload.outcomeToken === 'string' &&
                    response.payload.tests.constructor === Array
                ) {                
                    return response.payload
                } else {
                    console.error(`Failed receiving list of tests for simulation ${simulationToken}`, response)
                    throw new RunException(false, "Failed receiving list of tests for simulation")
                }
            },
            // Log request error (won't catch already logged inner error)
            reason => {
                console.log(`Failed to retrieve configuration parameters for simulation ${simulationToken}`,  reason)
                throw reason
            }
        )
}

async function sendResultsToServer(simOutcome, outcomeToken) {
    return await new ServerRequest(`${process.env.REACT_APP_SIMULATION_SERVER}/test/simulations/finish-simulation?outcomeToken=${outcomeToken}`)
        .awaitNoContentResponse()
        .post()
        .catch(sendFailed => {
            console.error('Failed send results of simulations to server', sendFailed)
            throw sendFailed
        })
}

async function sendHeartbeatToServer(outcomeToken) {
    return await new ServerRequest(`${process.env.REACT_APP_SIMULATION_SERVER}/test/simulations/heartbeat?outcomeToken=${outcomeToken}`)
        .awaitNoContentResponse()
        .post()
        .catch(sendFailed => {
            console.error('Failed send heartbeat to server', sendFailed)
            throw sendFailed
        })
}

function startHeartbeat(outcomeToken) {
    heartbeatId = setInterval(sendHeartbeatToServer, heartbeatFrequency, outcomeToken)
}

function stoptHeartbeat() {
    clearInterval(heartbeatId)
}
