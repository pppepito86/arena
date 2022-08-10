export const getPointsColor = (ratio: number): string => {
    return ratio === 1
        ? 'hsl(91 94% 61%)'
        : ratio > 0.9
        ? 'hsl(82deg 95% 61%)'
        : ratio > 0.8
        ? 'hsl(74deg 96% 61%)'
        : ratio > 0.7
        ? 'hsl(66deg 97% 61%)'
        : ratio > 0.6
        ? 'hsl(58deg 99% 62%)'
        : ratio > 0.5
        ? 'hsl(52deg 100% 66%)'
        : ratio > 0.4
        ? 'hsl(44deg 100% 69%)'
        : ratio > 0.3
        ? 'hsl(33deg 100% 71%)'
        : // : ratio > 0.2
          // ? 'hsl(24deg 100% 73%)'
          // : ratio > 0.1
          // ? 'hsl(15deg 100% 74%)'
          // : ratio > 0
          // ? 'hsl(7deg 100% 76%)'
          'transparent';
    //   ratio === 0 ? 'hsl(0deg 100% 78%)' :
    //     '#FFFFFF';
};
