import React from 'react';
import '../styling/Button.scss';
import '../styling/Sidebar.css';


function Sidebar({currentPage, setCurrentPage}) {

    function changePage(i) {
        setCurrentPage(i)
    }
    return (
        <div className="Sidebar">
            <ul>
                <li className={currentPage === 0? "active":""} onClick={() => setCurrentPage(0)}>
                    <span>Dashboard</span>
                </li>
                <li className={currentPage === 1? "active":""} onClick={() => setCurrentPage(1)}>
                    <span>Flows</span>
                </li>
                <li className={currentPage === 2? "active":""} onClick={() => setCurrentPage(2)}>
                    <span>Vault</span>
                </li>
            </ul>
        </div>
    );
}

export default Sidebar