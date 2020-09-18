import React from 'react';
import '../styling/Vault.css';
import ReactTypingEffect from 'react-typing-effect';

// const ReactTypingEffectDemo = () => {
//     return (
//         <ReactTypingEffect
//             text="Not yet complete ... unfortunately!" //text=["Hello.", "World!"]
//         />
//     );
// };
function Vault() {

    return (

        <div className="tc w-100 white">
            <div className="spinner spinner--steps icon-spinner" aria-hidden="true"></div>
            <h1>
            <ReactTypingEffect
                text="Not yet complete ... unfortunately!"
                speed={50}
                typingDelay={500}
                eraseDelay={10000}
            />
            </h1>
        </div>
    );
}
export default Vault;
