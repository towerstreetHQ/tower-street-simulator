import React from 'react';
import PageWrapper from './PageWrapper';

const CompletedTestScreen = () => {
  return (
    <PageWrapper>
      <>
        <h1 className="headline">
          <span className="color-success">Security scan completed</span>
        </h1>
        <div className="sub">You may now close this window.</div>
      </>
    </PageWrapper>
  );
};

export default CompletedTestScreen;
