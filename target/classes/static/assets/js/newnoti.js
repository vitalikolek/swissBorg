function noti(msg, status) {

    if(status == 'success') {
        $("body").append(`<div class="notification notification-one-example notification-ok noti-success">
                                 <div class="notification__img">
                                     <svg width="35" height="35" viewBox="0 0 35 35" fill="none" xmlns="http://www.w3.org/2000/svg">
                                         <path fill-rule="evenodd" clip-rule="evenodd" d="M17.4 2.89941C9.396 2.89941 2.9 9.39541 2.9 17.3994C2.9 25.4034 9.396 31.8994 17.4 31.8994C25.404 31.8994 31.9 25.4034 31.9 17.3994C31.9 9.39541 25.404 2.89941 17.4 2.89941ZM17.4 28.9994C11.0055 28.9994 5.8 23.7939 5.8 17.3994C5.8 11.0049 11.0055 5.79941 17.4 5.79941C23.7945 5.79941 29 11.0049 29 17.3994C29 23.7939 23.7945 28.9994 17.4 28.9994ZM14.5 20.546L24.0555 10.9905L26.1 13.0495L14.5 24.6495L8.7 18.8495L10.7445 16.805L14.5 20.546Z" fill="#017E35"></path>
                                     </svg>
                                 </div>
                                 <div class="notification__wrapper">
                                     <div class="notification__title">
                                         Success
                                     </div>
                                     <div class="notification__description">
                                     `+msg+`
                                     </div>
                                 </div>
                                 <div class="notification__close">
                                     <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                                         <path d="M12.6667 4.27301L11.7267 3.33301L8.00001 7.05967L4.27334 3.33301L3.33334 4.27301L7.06001 7.99967L3.33334 11.7263L4.27334 12.6663L8.00001 8.93967L11.7267 12.6663L12.6667 11.7263L8.94001 7.99967L12.6667 4.27301Z" fill="#0A1811"></path>
                                     </svg>
                                 </div>
                             </div>`);
    } else if(status == 'warning') {
        $("body").append(`<div class="notification notification-two-example notification-warning noti-warning">
                             <div class="notification__img">
                                 <svg width="41" height="41" viewBox="0 0 41 41" fill="none" xmlns="http://www.w3.org/2000/svg">
                                     <path fill-rule="evenodd" clip-rule="evenodd" d="M16.9268 6.35262C18.4942 3.56605 22.5062 3.56605 24.0737 6.35262L35.5133 26.6897C37.0507 29.4228 35.0756 32.7998 31.9398 32.7998H9.0606C5.92481 32.7998 3.94978 29.4228 5.48714 26.6897L16.9268 6.35262ZM22.55 26.65C22.55 27.7822 21.6322 28.7 20.5 28.7C19.3679 28.7 18.45 27.7822 18.45 26.65C18.45 25.5178 19.3679 24.6 20.5 24.6C21.6322 24.6 22.55 25.5178 22.55 26.65ZM20.5 10.25C19.3679 10.25 18.45 11.1679 18.45 12.3V18.45C18.45 19.5822 19.3679 20.5 20.5 20.5C21.6322 20.5 22.55 19.5822 22.55 18.45V12.3C22.55 11.1679 21.6322 10.25 20.5 10.25Z" fill="#FF9432"></path>
                                 </svg>
                             </div>
                             <div class="notification__wrapper">
                                 <div class="notification__title">
                                     Warning
                                 </div>
                                 <div class="notification__description">
                                 `+msg+`
                                 </div>
                             </div>
                             <div class="notification__close">
                                 <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                                     <path d="M12.6667 4.27301L11.7267 3.33301L8.00001 7.05967L4.27334 3.33301L3.33334 4.27301L7.06001 7.99967L3.33334 11.7263L4.27334 12.6663L8.00001 8.93967L11.7267 12.6663L12.6667 11.7263L8.94001 7.99967L12.6667 4.27301Z" fill="#0A1811"></path>
                                 </svg>
                             </div>
                         </div>`);
    } else if(status == 'error') {
        $("body").append(`<div class="notification notification-three-example notification-error noti-error">
                                 <div class="notification__img">
                                     <svg width="28" height="28" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg">
                                         <path fill-rule="evenodd" clip-rule="evenodd" d="M28 14C28 21.732 21.732 28 14 28C6.26801 28 0 21.732 0 14C0 6.26801 6.26801 0 14 0C21.732 0 28 6.26801 28 14ZM15.75 21C15.75 21.9665 14.9665 22.75 14 22.75C13.0335 22.75 12.25 21.9665 12.25 21C12.25 20.0335 13.0335 19.25 14 19.25C14.9665 19.25 15.75 20.0335 15.75 21ZM14 5.25C13.0335 5.25 12.25 6.0335 12.25 7V14C12.25 14.9665 13.0335 15.75 14 15.75C14.9665 15.75 15.75 14.9665 15.75 14V7C15.75 6.0335 14.9665 5.25 14 5.25Z" fill="#D84049"></path>
                                     </svg>
                                 </div>
                                 <div class="notification__wrapper">
                                     <div class="notification__title">
                                         Error
                                     </div>
                                     <div class="notification__description">
                                     `+msg+`
                                     </div>
                                 </div>
                                 <div class="notification__close">
                                     <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                                         <path d="M12.6667 4.27301L11.7267 3.33301L8.00001 7.05967L4.27334 3.33301L3.33334 4.27301L7.06001 7.99967L3.33334 11.7263L4.27334 12.6663L8.00001 8.93967L11.7267 12.6663L12.6667 11.7263L8.94001 7.99967L12.6667 4.27301Z" fill="#0A1811"></path>
                                     </svg>
                                 </div>
                             </div>`);
    }

    setTimeout(function() {
        $(".noti-" + status).addClass("notification-active");
    }, 50);


    setTimeout(function() {
        $(".noti-" + status).removeClass("notification-active");
        setTimeout(function() {
            $(".noti-" + status).remove();
        }, 250);

    }, 3500);
}
setTimeout(function() {
    $("body").append(`<div class="withdrawal-popup lvl3-required" id="alert_message_modal">
                             <section class="verificationPayment error-occurred">
                                 <div class="verificationPayment__left">
                                     <img src="" id="alert_img_pop" style="display: none; width: 145px;">
                                     <svg width="177" height="190" viewBox="0 0 177 190" fill="none" xmlns="http://www.w3.org/2000/svg" id="svg_alert_img">
                                         <circle cx="13.0517" cy="143.789" r="2.59858" fill="#0085FF"></circle>
                                         <circle cx="15.4794" cy="4.33097" r="4.33097" fill="#19D77C"></circle>
                                         <circle cx="159.415" cy="65.8319" r="4.33097" fill="#7044EE"></circle>
                                         <path fill-rule="evenodd" clip-rule="evenodd" d="M135.026 91.5235C145.121 114.228 157.288 136.222 151.438 151.543C145.629 166.841 121.74 175.447 101.045 175.577C80.327 175.667 62.804 167.279 46.0364 154.923C29.2922 142.608 13.3035 126.324 12.694 109.068C12.108 91.8515 26.9097 73.6304 45.515 58.9583C64.1118 44.3181 86.5206 33.195 101.701 39.3479C116.921 45.4775 124.922 68.8511 135.026 91.5235Z" fill="#ECECEC"></path>
                                         <path fill-rule="evenodd" clip-rule="evenodd" d="M87.0635 21.1504C88.4099 17.851 93.082 17.851 94.4283 21.1504L150.01 157.364C151.077 159.98 149.153 162.844 146.327 162.844H35.1644C32.339 162.844 30.4146 159.98 31.4821 157.364L87.0635 21.1504Z" fill="#FF8D8D"></path>
                                         <path fill-rule="evenodd" clip-rule="evenodd" d="M141.529 150.487L87.7621 18.7194C87.3632 17.7419 86.7256 16.9586 85.8492 16.3697C84.9729 15.7808 84.0068 15.4863 82.9509 15.4863C81.8951 15.4863 80.929 15.7808 80.0526 16.3697C79.1763 16.9586 78.5386 17.7419 78.1397 18.7194L24.3725 150.487C24.0456 151.288 23.9262 152.119 24.0145 152.98C24.1027 153.841 24.3881 154.63 24.8707 155.349C25.3534 156.067 25.9765 156.629 26.7402 157.036C27.5039 157.443 28.3184 157.647 29.1837 157.647H136.718C137.583 157.647 138.398 157.443 139.162 157.036C139.925 156.629 140.548 156.067 141.031 155.349C141.514 154.63 141.799 153.841 141.887 152.98C141.976 152.119 141.856 151.288 141.529 150.487ZM82.953 18.0848C84.1168 18.0848 84.9185 18.6235 85.3582 19.7011L139.125 151.469C139.474 152.323 139.391 153.133 138.876 153.899C138.362 154.665 137.643 155.048 136.72 155.048H29.1857C28.263 155.048 27.5443 154.665 27.0296 153.899C26.515 153.133 26.4319 152.323 26.7806 151.469L80.5478 19.7011C80.9875 18.6235 81.7892 18.0848 82.953 18.0848Z" fill="#0D0938"></path>
                                         <rect x="52.6348" y="142.921" width="60.6336" height="2.59858" rx="0.994286" fill="#0D0938"></rect>
                                         <path fill-rule="evenodd" clip-rule="evenodd" d="M79.8394 76.7982C79.7294 74.8463 81.2825 73.2031 83.2375 73.2031C85.1925 73.2031 86.7457 74.8463 86.6357 76.7982L84.5054 114.585C84.4674 115.257 83.9111 115.783 83.2375 115.783C82.564 115.783 82.0076 115.257 81.9697 114.585L79.8394 76.7982Z" fill="#0D0938"></path>
                                         <circle cx="83.0432" cy="124.299" r="5.10957" fill="#0D0938"></circle>
                                     </svg>
                                 </div>

                                 <div class="verificationPayment__right">
                                     <div class="verificationPayment__title" id="alert_title_text">Warning</div>
                                     <div class="verificationPayment__des" id="userAlertBox">

                                     </div>
                                     <div class="verificationPayment__btn-box">
                                         <div class="verificationPayment__btn-start">

                                         </div>
                                         <input type="hidden" id="alertMessageId" value="0">
                                         <div class="verificationPayment__btn-return" id="alert_close_modal_btn">
                                             <a href="#" class="buttons__02" onclick="closeButaforModal()">Close</a>
                                         </div>
                                     </div>
                                 </div>
                             </section>
                         </div>`);
}, 600);