import React from 'react';
import IconShield from '../img/icons/icon-warning.svg';
import PageWrapper from './PageWrapper';

const CrashedTestScreen = () => (
  <PageWrapper>
    <>
      <img src={IconShield} alt="icon-shield" />
      <h1 className="headline">
        <span className="color-alert">Security scan can't be completed</span>
      </h1>
      <div className="sub">
        Something went wrong with the test. Send link to this webpage to your security manager.
      </div>
    </>
  </PageWrapper>
);

export default CrashedTestScreen;
