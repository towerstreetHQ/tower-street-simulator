import React, { Component } from 'react';
import BrokenLinkTestScreen from './BrokenLinkTestScreen';
import CompletedTestScreen from './CompletedTestScreen';
import CrashedTestScreen from './CrashedTestScreen';
import InvalidBrowserScreen from './InvalidBrowserScreen';
import PendingTestScreen from './pendingTest/PendingTestScreen';

export const TestState = {
  TEST_IN_PROGRESS: 0,
  TEST_COMPLETED: 1,
  BROKEN_LINK: 2,
  TEST_CRASHED: 3,
  INVALID_BROWSER: 4,
};

interface Label {
  label: string;
}

interface Props {
  progress: number;
  startedTests: Label[];
  testState: number;
}

class TestScreen extends Component<Props> {
  render() {
    const { testState } = this.props;
    return (
      <>
        {testState === TestState.TEST_IN_PROGRESS && (
          <PendingTestScreen
            progress={this.props.progress}
            startedTests={this.props.startedTests}
          />
        )}
        {testState === TestState.TEST_COMPLETED && <CompletedTestScreen />}
        {testState === TestState.BROKEN_LINK && <BrokenLinkTestScreen />}
        {testState === TestState.TEST_CRASHED && <CrashedTestScreen />}
        {testState === TestState.INVALID_BROWSER && <InvalidBrowserScreen />}
      </>
    );
  }
}

export default TestScreen;
