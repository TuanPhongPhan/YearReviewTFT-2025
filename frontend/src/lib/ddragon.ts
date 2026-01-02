export const DDRAGON_VERSION = "15.24.1"; // later: make dynamic
export const DDRAGON_LOCALE = "en_US";

export const ddragon = {
    itemData: `https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}/data/${DDRAGON_LOCALE}/tft-item.json`,
    traitData: `https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}/data/${DDRAGON_LOCALE}/tft-trait.json`,
    champData: `https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}/data/${DDRAGON_LOCALE}/tft-champion.json`,

    itemImg: (full: string) =>
        `https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}/img/tft-item/${full}`,
    traitImg: (full: string) =>
        `https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}/img/tft-trait/${full}`,
    champImg: (full: string) =>
        `https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}/img/tft-champion/${full}`,

};
