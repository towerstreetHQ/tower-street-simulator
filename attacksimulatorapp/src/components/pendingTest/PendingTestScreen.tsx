import React from 'react';
import IconWarning from '../../img/icons/icon-warning.svg';
import InterestingFacts from './InterestingFacts';
import TestProgressBar from './TestProgressBar';

export const uniqueTests = (testNames: TestName[]): TestName[] => {
  const res = [] as TestName[];
  testNames.forEach((test) => {
    if (!res.length || res[res.length - 1].label !== test.label) {
      res.push(test);
    }
  });
  return res;
};

interface TestName {
  label: string;
}
interface Props {
  progress: number;
  startedTests: TestName[];
}
const PendingTestScreen = ({ progress, startedTests }: Props) => {
  const campaign = process.env.REACT_APP_SIMULATOR_CAMPAIGN_MODE;
  const testsToRender = uniqueTests(startedTests).slice(0, 4);

  return (
    <>
      <div className="section pb-0">
        <div className="grid-container">
          <div className="grid-x">
            <div className="cell medium-6">
              <TestProgressBar value={progress} />
            </div>
            <div className="cell medium-6">
              <h1 className="headline mb-20 ml-20">
                <strong>...scanning</strong>
              </h1>
              <div className="callout alert mb-15">
                <h2 className="headline">
                  <div className="grid-x">
                    <div className="cell shrink"><img src={IconWarning} alt="" /></div>
                    <div className="cell auto">
                      Please don’t close this window or turn off your computer<br className="show-for-large" />
                      <strong> until the scan has finished.</strong>
                    </div>
                  </div>
                </h2>
              </div>
              <ul className="list-progress">
                {testsToRender.map((test, index) => (
                  <li key={index} className={index ? 'done' : ''}>
                    {test.label}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      </div>
      {campaign && (
        <div className="section section--interesting-facts">
          <div className="grid-container">
            <InterestingFacts />
          </div>
        </div>
      )}
    </>
  );
};

export default PendingTestScreen;
