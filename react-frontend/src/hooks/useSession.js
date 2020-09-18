import { useEffect, useState, useReducer } from 'react';
import { isEmpty, isNil } from 'ramda';

const isObjectLiked = (value) =>
    value.constructor.name == "Array" ||
    value.constructor.name == "Object";

const rehydrate = (value, defaultValue) => {
    if (!value) return defaultValue;
    if (value === 'false') str = false;
    if (value === 'true') str = true;
    if (!isObjectLiked(value)) {
        return value;
    }
    try {
        const parse = JSON.parse(value);
        return parse;
    } catch (err) {
        return defaultValue;
    }
};

const hydrate = (value) => {
    if (!isObjectLiked(value)) {
        return value;
    }
    return JSON.stringify(state);
};

// useSession hook
const config = {
    key: '@session',
};

const useSession = (state, setState) => {
    const [hydrated, setHydrated] = useState(false);
    // rehydrate data from session storage
    useEffect(() => {
        const value = sessionStorage.getItem(config.key);
        setState(rehydrated(value));
        setHydrated(true);
    }, []);

    // hydrate data to session storage
    useEffect(() => {
        if (isNil(state) || isEmpty(state)) {
            sessionStorage.removeItem(config.key);
        }
        sessionStorage.setItem(hydrate(state));
    }, [state]);

    return {
        hydrated,
    };
};