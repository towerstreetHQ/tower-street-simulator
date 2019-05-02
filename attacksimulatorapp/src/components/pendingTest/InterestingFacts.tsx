import Flickity from 'flickity';
import * as React from 'react';
interface Fact {
  description: string;
  source: string;
}

const facts: Fact[] = [
  {
    description:
      'Cybercrime damages will cost the world $6 trillion annually by 2021 – exponentially more than the damage inflicted from natural disasters in a year.',
    source: 'Cybersecurity Ventures',
  },
  {
    description:
      'Around $76 billion of illegal activity per year involves bitcoin, which is close to the scale of the U.S. and European markets for illegal drugs.',
    source: 'University of Sydney',
  },
  {
    description:
      'The “Cyber’s Most Wanted” list on the FBI website features 63 notorious people (up from 19 in 2016) that have conspired to commit the most damaging crimes against the U.S.',
    source: 'FBI',
  },
  { description: 'Asia-Pacific companies receive 6 cyber threats every minute.', source: 'Cisco' },
  {
    description:
      'The 5 biggest data breaches of all time (including year and the number of records hacked): Yahoo, 3 billion, 2013; Marriott, 500 million, 2014-2018; Adult FriendFinder, 412 million (2016); MySpace, 360 million (2016); Under Armor, 150 million (2018).',
    source: 'Quartz',
  },
  {
    description:
      'The 5 most cyber-attacked industries over the past 5 years are healthcare, manufacturing, financial services, government, and transportation.',
    source: 'Cybersecurity Ventures',
  },
  {
    description:
      'Global ransomware damage costs are predicted to hit $20 billion in 2021, up from $11.5 billion in 2019.',
    source: 'Cybersecurity Ventures',
  },
  {
    description:
      'A report from CTA indicates a massive 459% increase in the rate of illegal crypto jacking, through which hackers hijack computer processing power to mine cryptocurrencies like bitcoin and Monero.',
    source: 'Cyber Threat Alliance (CTA)',
  },
  {
    description:
      'The World Wide Web was invented in 1989. The first-ever website went live in 1991. Today there are more than 1.9 billion websites.',
    source: 'Wikipedia',
  },
  {
    description:
      'Smartphones will account for more than 55 percent of total IP traffic by 2025, and Wi-Fi and mobile devices will account for nearly 80 percent of IP traffic by that time.',
    source: 'Cisco',
  },
  {
    description: 'The number of connected devices on the Internet will exceed 50 billion by 2020.',
    source: 'Cisco',
  },
  {
    description:
      'Ransomware attacks on healthcare organizations are predicted to quadruple between 2017 and 2020, and will grow to 5X by 2021.',
    source: 'Cybersecurity Ventures',
  },
];

const Fact = ({ fact: { description, source } }: { fact: Fact }) => (
  <div className="carousel-cell">
    <blockquote className="box-quote">
      <h6>Some interesting facts in the meantime:</h6>
      {description}
      <div className="source">Source: {source}</div>
    </blockquote>
  </div>
);

const options = {
  cellAlign: 'center',
  contain: true,
  pageDots: true,
  wrapAround: true,
  cellSelector: '.carousel-cell',
  prevNextButtons: true,
  autoPlay: 6000,
  arrowShape: {
    x0: 10,
    x1: 60,
    y1: 50,
    x2: 65,
    y2: 45,
    x3: 20,
  },
};

const InterestingFacts = () => {
  React.useEffect(() => {
    const carousel = document.querySelector('.carousel');
    if (document.createElement && carousel) {
      const flickity = new Flickity(carousel, options);
      flickity.resize();
    }
  }, []);

  return (
    <div className="carousel carousel--quotes text-center">
      {facts.map((fact, index) => (
        <Fact fact={fact} key={`key${index}`} />
      ))}
    </div>
  );
};

export default InterestingFacts;
