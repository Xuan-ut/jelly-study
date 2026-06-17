var JellyStudyApp = (function() {
    var currentUser = null;

    function init() {
        var savedUser = localStorage.getItem('jellystudy_user');
        if (savedUser) {
            try {
                currentUser = JSON.parse(savedUser);
            } catch(e) {
                localStorage.removeItem('jellystudy_user');
            }
        }
        updateNavUI();
    }

    function getUser() {
        if (!currentUser) {
            var savedUser = localStorage.getItem('jellystudy_user');
            if (savedUser) {
                try {
                    currentUser = JSON.parse(savedUser);
                } catch(e) {
                    localStorage.removeItem('jellystudy_user');
                }
            }
        }
        return currentUser;
    }

    function getUserId() {
        return currentUser ? currentUser.id : null;
    }

    function isLoggedIn() {
        var user = getUser();
        return user !== null && user.role !== 'GUEST';
    }

    function isGuest() {
        var user = getUser();
        return user !== null && user.role === 'GUEST';
    }

    function updateNavUI() {
        var loginBtn = document.getElementById('nav-login-btn');
        var userInfo = document.getElementById('nav-user-info');
        var userAvatar = document.getElementById('nav-user-avatar');
        var planLink = document.getElementById('nav-plan-link');
        var dropdownNickname = document.getElementById('dropdown-nickname');
        var dropdownRole = document.getElementById('dropdown-role');

        if (currentUser) {
            if (loginBtn) loginBtn.style.display = 'none';
            if (userInfo) userInfo.style.display = 'flex';
            if (userAvatar) {
                if (currentUser.avatar) {
                    userAvatar.innerHTML = '<img src="' + currentUser.avatar + '" style="width:100%;height:100%;object-fit:cover;border-radius:50%;">';
                } else {
                    userAvatar.textContent = (currentUser.nickname || currentUser.username || 'U').charAt(0).toUpperCase();
                }
            }
            if (planLink) {
                planLink.style.display = '';
                if (currentUser.role === 'GUEST') {
                    planLink.title = '登录后可使用学习计划功能';
                }
            }
            if (dropdownNickname) dropdownNickname.textContent = currentUser.nickname || currentUser.username || '用户';
            if (dropdownRole) dropdownRole.textContent = currentUser.role === 'GUEST' ? '游客' : '正式用户';
        } else {
            if (loginBtn) loginBtn.style.display = '';
            if (userInfo) userInfo.style.display = 'none';
            if (planLink) planLink.style.display = '';
        }
    }

    function login(username, password) {
        return axios.post('/api/users/login', { username: username, password: password })
            .then(function(response) {
                if (response.data.success) {
                    currentUser = response.data.user;
                    localStorage.setItem('jellystudy_user', JSON.stringify(currentUser));
                    updateNavUI();
                    return { success: true, user: currentUser };
                } else {
                    return { success: false, message: response.data.message || '登录失败' };
                }
            })
            .catch(function(error) {
                return { success: false, message: error.response?.data?.message || '网络错误' };
            });
    }

    function register(username, password, nickname) {
        return axios.post('/api/users/register', { username: username, password: password, nickname: nickname })
            .then(function(response) {
                if (response.data.success) {
                    return { success: true };
                } else {
                    return { success: false, message: response.data.message || '注册失败' };
                }
            })
            .catch(function(error) {
                return { success: false, message: error.response?.data?.message || '网络错误' };
            });
    }

    function guestLogin() {
        return axios.post('/api/users/guest')
            .then(function(response) {
                if (response.data.success) {
                    currentUser = response.data.user;
                    localStorage.setItem('jellystudy_user', JSON.stringify(currentUser));
                    updateNavUI();
                    return { success: true, user: currentUser };
                } else {
                    return { success: false, message: response.data.message || '游客登录失败' };
                }
            })
            .catch(function(error) {
                return { success: false, message: '网络错误' };
            });
    }

    function logout() {
        currentUser = null;
        localStorage.removeItem('jellystudy_user');
        updateNavUI();
        if (typeof afterLogout === 'function') afterLogout();
    }

    function requireLogin(callback) {
        if (isLoggedIn()) {
            callback(currentUser);
        } else if (isGuest()) {
            showUpgradeModal();
        } else {
            showLoginModal();
        }
    }

    function showLoginModal() {
        closeAllModals();
        var modal = document.getElementById('modal-login');
        if (modal) modal.classList.add('show');
    }

    function showRegisterModal() {
        closeAllModals();
        var modal = document.getElementById('modal-register');
        if (modal) modal.classList.add('show');
    }

    function showUpgradeModal() {
        closeAllModals();
        var modal = document.getElementById('modal-upgrade');
        if (modal) modal.classList.add('show');
    }

    function closeAllModals() {
        document.querySelectorAll('.js-modal').forEach(function(m) {
            m.classList.remove('show');
        });
    }

    function handleLoginSubmit(e) {
        e.preventDefault();
        var username = document.getElementById('input-login-username').value.trim();
        var password = document.getElementById('input-login-password').value.trim();
        if (!username || !password) { alert('请填写用户名和密码'); return; }

        login(username, password).then(function(result) {
            if (result.success) {
                closeAllModals();
                if (typeof afterLogin === 'function') afterLogin(result.user);
            } else {
                alert(result.message);
            }
        });
    }

    function handleRegisterSubmit(e) {
        e.preventDefault();
        var username = document.getElementById('input-reg-username').value.trim();
        var password = document.getElementById('input-reg-password').value.trim();
        var nickname = document.getElementById('input-reg-nickname').value.trim();
        if (!username || !password) { alert('请填写用户名和密码'); return; }

        register(username, password, nickname).then(function(result) {
            if (result.success) {
                alert('注册成功！请登录');
                showLoginModal();
            } else {
                alert(result.message);
            }
        });
    }

    function handleGuestLogin() {
        guestLogin().then(function(result) {
            if (result.success) {
                closeAllModals();
                if (typeof afterLogin === 'function') afterLogin(result.user);
            } else {
                alert(result.message);
            }
        });
    }

    function recordActivity(activityType, targetId, targetTitle, content, metadata) {
        var user = getUser();
        if (!user || user.role === 'GUEST') return;
        var activity = {
            userId: user.id,
            activityType: activityType,
            targetId: targetId || '',
            targetTitle: targetTitle || '',
            content: content || ''
        };
        if (metadata) {
            activity.metadata = metadata;
        }
        axios.post('/api/activities', activity).catch(function(error) {
            console.warn('记录用户行为失败:', error.message);
        });
    }

    return {
        init: init,
        getUser: getUser,
        getUserId: getUserId,
        isLoggedIn: isLoggedIn,
        isGuest: isGuest,
        login: login,
        register: register,
        guestLogin: guestLogin,
        logout: logout,
        requireLogin: requireLogin,
        showLoginModal: showLoginModal,
        showRegisterModal: showRegisterModal,
        showUpgradeModal: showUpgradeModal,
        closeAllModals: closeAllModals,
        handleLoginSubmit: handleLoginSubmit,
        handleRegisterSubmit: handleRegisterSubmit,
        handleGuestLogin: handleGuestLogin,
        updateNavUI: updateNavUI,
        recordActivity: recordActivity
    };
})();

document.addEventListener('DOMContentLoaded', function() {
    JellyStudyApp.init();

    document.querySelectorAll('.js-modal-overlay').forEach(function(overlay) {
        overlay.addEventListener('click', function(e) {
            if (e.target === overlay) JellyStudyApp.closeAllModals();
        });
    });
});

var afterLogin = null;
var afterLogout = null;
