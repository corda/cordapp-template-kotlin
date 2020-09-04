import { useState } from 'react';

const useModal = () => {
    const [isShowing, setIsShowing] = useState(false);
    const [flowData, setFlowData] = useState([]);

    function toggle() {
        setIsShowing(!isShowing);
    }

    function setModalData(modalData) {
        setFlowData(modalData)
    }

    return {
        isShowing,
        flowData,
        toggle,
        setModalData

    }
};

export default useModal;