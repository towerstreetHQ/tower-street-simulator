import React from 'react';
import { supportedBrowsers, getBrowserData } from '../Browser';
import IconWarning from '../img/icons/icon-warning.svg';
import PageWrapper from './PageWrapper';

const browserData = getBrowserData();

const InvalidBrowserScreen = () => {
  return (
    <PageWrapper>
      <>
        <h1 className="headline">
          <strong>
            Unsupported <br className="show-for-medium" />
            browser detected
          </strong>
        </h1>
        <div className="callout alert">
          {browserData && (
            <h2 className="headline">
              <img src={IconWarning} alt="icon-warning" /> Currently used unsupported browser:
              {browserData.browser.name} {browserData.browser.version}
            </h2>
          )}
          <p>
            Please re-run the security scan in the latest version
            <br className="show-for-medium" /> of one of the following supported browsers.
          </p>
        </div>
        <ul className="list-browsers">
          {supportedBrowsers.map((browser, index) => (
            <li key={index}>
              <img src={browser.logo} alt={browser.id} width="32px" />
              {browser.name} version {browser.version} or newer
            </li>
          ))}
        </ul>
      </>
    </PageWrapper>
  );
};

export default InvalidBrowserScreen;
