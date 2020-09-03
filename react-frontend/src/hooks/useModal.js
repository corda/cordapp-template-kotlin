import { useState } from 'react';

const useModal = () => {
    const [isShowing, setIsShowing] = useState(false);
    const [displayData, setDisplayData] = useState("");

    function toggle() {
        setIsShowing(!isShowing);
    }

    function setModalData(modalData) {
        setDisplayData(modalData)
    }

    return {
        isShowing,
        displayData,
        toggle,
        setModalData

    }
};

export default useModal;