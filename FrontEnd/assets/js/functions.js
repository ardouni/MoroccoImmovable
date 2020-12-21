const copyright = 'Copyright &copy; 2020 Morocco Immovable';
document.getElementById('copyright').innerHTML = copyright;
const apiUrl = 'http://localhost:8080';

function login() {
    let email = document.getElementById('email').value;
    let password = document.getElementById('password').value;
    if (email.toString().trim() === '' || password.toString().trim() === '') {
        toastr.error('The fields are empty !', 'Morocco Immovable', {
            timeOut: 8500,
            positionClass: 'toast-bottom-center'
        });
    } else {
        axios.post(apiUrl + '/login', {
            email: email,
            password: password,
        }).then(function (response) {
            if (response.data.message === '1') {
                toastr.success('Logged in successfully !', 'Morocco Immovable', {
                    timeOut: 8500,
                    positionClass: 'toast-bottom-center'
                });
                cookie.set('token', response.data.token);
                window.location.href = "index.html";
            } else if (response.data.message === '2') {
                toastr.warning('You have not verified your email yet !', 'Morocco Immovable', {
                    timeOut: 8500,
                    positionClass: 'toast-bottom-center'
                });
            } else {
                toastr.error('The given information are wrong !', 'Morocco Immovable', {
                    timeOut: 8500,
                    positionClass: 'toast-bottom-center'
                });
            }
        }).catch(function (error) {
            console.log(error);
        });
    }
}

function sendMessage() {
    let name = document.getElementById('name').value;
    let email = document.getElementById('email').value;
    let subject = document.getElementById('subject').value;
    let message = document.getElementById('message').value;
    if (subject.toString().trim() === '' || message.toString().trim() === '' || name.toString().trim() === '' || email.toString().trim() === '') {
        toastr.error('The fields are empty !', 'Morocco Immovable', {
            timeOut: 8500,
            positionClass: 'toast-bottom-center'
        });
    } else {
        axios.post(apiUrl + '/contact', {
            name: name,
            email: email,
            subject: subject,
            message: message,
        }, {headers: {"Authorization": cookie.get('token')}}).then(function (response) {
            if (response.data.message === '1') {
                toastr.success('Your message has been sent successfully !', 'Morocco Immovable', {
                    timeOut: 8500,
                    positionClass: 'toast-bottom-center'
                });
            } else {
                toastr.error('An error occurred, please try again later !', 'Morocco Immovable', {
                    timeOut: 8500,
                    positionClass: 'toast-bottom-center'
                });
            }
        }).catch(function (error) {
            console.log(error);
        });
    }
}

function logout() {
    cookie.remove('token');
    window.location.href = "login.html";
}

function goToRegisterPage() {
    window.location.href = "register.html";
}

function register() {
    let firstName = document.getElementById('firstName').value;
    let lastName = document.getElementById('lastName').value;
    let email = document.getElementById('email').value;
    let password = document.getElementById('password').value;
    let identityCode = document.getElementById('identityCode').value;
    let phoneNumber = document.getElementById('phoneNumber').value;
    let age = document.getElementById('age').value;
    let city = document.getElementById('city').value;
    if (firstName.toString().trim() === '' || lastName.toString().trim() === '' || email.toString().trim() === '' || password.toString().trim() === '' || identityCode.toString().trim() === '' || phoneNumber.toString().trim() === '' || age.toString().trim() === '' || city.toString().trim() === '') {
        toastr.error('The fields are empty !', 'Morocco Immovable', {
            timeOut: 8500,
            positionClass: 'toast-bottom-center'
        });
    } else {
        axios.post(apiUrl + '/register', {
            firstName: firstName,
            lastName: lastName,
            email: email,
            password: password,
            identityCode: identityCode,
            phoneNumber: phoneNumber,
            age: parseInt(age),
            city: city
        }).then(function (response) {
            if (response.data.message === '1') {
                toastr.success('Your account has been created successfully, please verify your email address !', 'Morocco Immovable', {
                    timeOut: 8500,
                    positionClass: 'toast-bottom-center'
                });
                setTimeout(function () {
                    window.location.href = "login.html";
                }, 2500);
            } else {
                toastr.error('An error occurred, please try again later !', 'Morocco Immovable', {
                    timeOut: 8500,
                    positionClass: 'toast-bottom-center'
                });
            }
        }).catch(function (error) {
            console.log(error);
        });
    }
}

function getHouses() {
    axios.get(apiUrl + '/houses', {
        headers: {"Authorization": cookie.get('token')}
    }).then(function (response) {
        for (var i = 0; i < response.data.length; i++) {
            add((i + 1), response.data[i]);
        }
    }).catch(function (error) {
        console.log(error);
    });
}

function listOwnerHouses() {
    axios.get(apiUrl + '/owner/houses', {
        headers: {"Authorization": cookie.get('token')}
    }).then(function (response) {
        if (response.data.length > 0) {
            for (var i = 0; i < response.data.length; i++) {
                addOwnerHouses((i + 1), response.data[i]);
            }
        } else {
            let all = document.getElementById('all-items');
            all.innerHTML = "<center><p></p></center>"
        }
    }).catch(function (error) {
        console.log(error);
    });
}

function search() {
    let query = document.getElementById('query').value;
    if (query.toString().trim() === '') {
        toastr.error('The fields are empty !', 'Morocco Immovable', {
            timeOut: 8500,
            positionClass: 'toast-bottom-center'
        });
    } else {
        axios.get(apiUrl + '/search/' + query, {
            headers: {"Authorization": cookie.get('token')}
        }).then(function (response) {
            if (response.data.length > 0) {
                document.getElementById('all-items').innerHTML = '';
                for (var i = 0; i < response.data.length; i++) {
                    add((i + 1), response.data[i]);
                }
            } else {
                document.getElementById('all-items').innerHTML = '<center><p>There is no result, please try again with another word !</p></center>';
            }
        }).catch(function (error) {
            console.log(error);
        });
    }
}

function add(i, element) {
    var type = '';
    if (element.type === 'rent') {
        type = 'Rent'
    } else {
        type = 'Buy'
    }
    let all = document.getElementById('all-items');
    var item = '<div class="col-lg-4" style="margin-bottom: 10px;">' +
        '<div class="trainer-item">' +
        '<div class="image-thumb">' +
        '<img src="http://localhost:8080/' + element.photo + '" alt="">' +
        '</div><div class="down-content">' +
        '<center><span>House ' + i + ' (For ' + element.type + ')</span><h4>' + element.price + ' DH</h4><p>' + element.description + '</p>' +
        '<button type="button" onclick="showDetails(' + element.id + ')" class="main-button">' + type + '</button>' +
        '</center></div></div></div>'

    all.innerHTML += item;
}

function addOwnerHouses(i, element) {
    var type = '';
    if (element.type === 'rent') {
        type = 'Has been rented'
    } else {
        type = 'Has been sold'
    }
    let all = document.getElementById('all-items');
    var item = '<div class="col-lg-4" style="margin-bottom: 10px;">' +
        '<div class="trainer-item">' +
        '<div class="image-thumb">' +
        '<img src="http://localhost:8080/' + element.photo + '" alt="">' +
        '</div><div class="down-content">' +
        '<center><span>House ' + i + ' (For ' + element.type + ')</span><h4>' + element.price + ' DH</h4><p>' + element.description + '</p>' +
        '<button type="button" onclick="rentedOrSold(' + element.id + ')" class="main-button">' + type + '</button>' +
        '</center></div></div></div>'

    all.innerHTML += item;
}

function showDetails(id) {
    axios.get(apiUrl + '/houses/' + id, {
        headers: {"Authorization": cookie.get('token')}
    }).then(function (response) {
        let owner = response.data;
        Swal.fire(
            'Owner Information',
            'The owner : ' + owner.firstName + ' ' + owner.lastName + '<br>' + 'His city : ' + owner.city + '<br>' + 'His email : ' + owner.email + '<br>' + 'His phone number : ' + owner.phoneNumber,
            'info'
        );
    }).catch(function (error) {
        console.log(error);
    });
}

function rentedOrSold(id) {
    axios.get(apiUrl + '/rentedOrSold/houses/' + id, {
        headers: {"Authorization": cookie.get('token')}
    }).then(function (response) {
        if (response.data.message === '1')
            window.location.href = "add.html";
    }).catch(function (error) {
        console.log(error);
    });
}

function getBase64(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => resolve(reader.result);
        reader.onerror = error => reject(error);
    });
}

function addHouse() {
    let price = document.getElementById('price').value;
    let type = document.getElementById('type').value;
    let description = document.getElementById('description').value;
    var file = document.querySelector('#housePhoto > input[type="file"]').files[0];
    if (price.toString().trim() === '' || type.toString().trim() === '' || description.toString().trim() === '' || file.toString().trim() === '') {
        toastr.error('The fields are empty !', 'Morocco Immovable', {
            timeOut: 8500,
            positionClass: 'toast-bottom-center'
        });
    } else {
        getBase64(file).then(
            function (data) {
                axios.post(apiUrl + '/add', {
                        price: parseFloat(price),
                        description: description,
                        type: type,
                        photo: data,
                    }, {headers: {"Authorization": cookie.get('token')}}
                ).then(function (response) {
                    if (response.data.message === '1') {
                        toastr.success('The operation has been completed successfully !', 'Morocco Immovable', {
                            timeOut: 8500,
                            positionClass: 'toast-bottom-center'
                        });
                        window.location.href = "add.html";
                    } else {
                        toastr.error('An error occurred, please try again later !', 'Morocco Immovable', {
                            timeOut: 8500,
                            positionClass: 'toast-bottom-center'
                        });
                    }
                }).catch(function (error) {
                    console.log(error);
                });
            }
        );
    }
}
