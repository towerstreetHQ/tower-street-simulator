import * as React from 'react';
import { checkBrowser } from './Browser';
import Layout from './components/Layout';
import TestScreen, { TestState } from './components/TestScreen';
import runFromServer, { getSimulationToken } from './framework/runFromServer';

class App extends React.Component<{}> {
  state = {
    simulationToken: null,
    outcomeToken: null,
    tests: [],
    startedTests: [],
    testProgress: 0,
    testState: TestState.TEST_IN_PROGRESS,
  };

  componentDidMount() {
    if (process.env.REACT_APP_AUTO_RUN_ENABLED) {
      if (checkBrowser()) {
        this.runWithSimulationToken();
      } else {
        this.setState({ testState: TestState.INVALID_BROWSER });
      }
    }
  }

  simulationStarted = (simulationToken: any, outcomeToken: any, tests: any) => {
    this.setState({ simulationToken, outcomeToken, tests });
  };

  testStarted = (test: any) => {
    const startedTests = [test, ...this.state.startedTests];
    const testProgress =
      this.state.tests.length > 0 ? (startedTests.length - 1) / this.state.tests.length : 0;

    this.setState({ startedTests, testProgress });
  };

  runWithSimulationToken = async () => {
    try {
      const token = await getSimulationToken();
      await runFromServer(token, this.simulationStarted, this.testStarted);
      this.setState({ testState: TestState.TEST_COMPLETED });
    } catch (err) {
      if (err !== undefined && err.brokenLink) {
        this.setState({ testState: TestState.BROKEN_LINK });
      } else {
        // tslint:disable-next-line no-console
        console.log(err);
        this.setState({ testState: TestState.TEST_CRASHED });
      }
    } finally {
      if (opener) {
        // auto-close simulator window if it was opened from another app (e.g. simulator campaign)
        close();
      }
    }
  };

  render() {
    return (
      <Layout lightVariant>
        <TestScreen
          progress={this.state.testProgress}
          startedTests={this.state.startedTests}
          testState={this.state.testState}
        />
        {!process.env.REACT_APP_AUTO_RUN_ENABLED ? (
          <div>
            <button onClick={this.runWithSimulationToken}>Run with simulation token</button>
          </div>
        ) : null}
      </Layout>
    );
  }
}

export default App;
