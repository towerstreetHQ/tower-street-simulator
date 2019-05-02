import React from 'react';
import IconShield from '../img/icons/icon-warning.svg';
import PageWrapper from './PageWrapper';

const BrokenLinkTestScreen = () => (
  <PageWrapper>
    <>
      <img src={IconShield} alt="icon-shield" />
      <h1 className="headline">
        <span className="color-alert">Security scan can't be completed</span>
      </h1>
      <div className="sub">
        Invalid link to security test - missing or wrong simulationToken parameter.{' '}
      </div>
    </>
  </PageWrapper>
);

export default BrokenLinkTestScreen;
