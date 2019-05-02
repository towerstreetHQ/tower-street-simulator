import * as React from 'react';

const PageWrapper = ({ children }: { children: React.ReactChildren | React.ReactChild }) => (
  <div className="section">
    <div className="grid-container">
      <div className="grid-x">
        <div className="cell large-7">{children}</div>
      </div>
    </div>
  </div>
);

export default PageWrapper;
