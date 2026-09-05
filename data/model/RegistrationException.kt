package com.example.data.model

sealed class RegistrationException(message: String) : Exception(message) {
    class DuplicatePhoneException(val phone: String) : 
        RegistrationException("این شماره موبایل قبلاً ثبت شده است.")
    
    class DuplicateNationalIdException(val nationalId: String) : 
        RegistrationException("این کد ملی قبلاً ثبت شده است.")

    class DuplicateUsernameException(val username: String) : 
        RegistrationException("این نام کاربری قبلاً توسط کاربر دیگری ثبت شده است.")
    
    class DuplicateBothException : 
        RegistrationException("کاربری با این شماره موبایل، کد ملی یا نام کاربری قبلاً ثبت شده است.")
    
    class InvalidPhoneException(val phone: String) : 
        RegistrationException("شماره موبایل وارد شده نامعتبر است. فرمت صحیح: 09xxxxxxxxx")
    
    class InvalidNationalIdException(val nationalId: String) : 
        RegistrationException("کد ملی وارد شده نامعتبر است.")

    class InvalidUsernameException(val username: String) : 
        RegistrationException("نام کاربری نامعتبر است. باید بین ۳ تا ۳۰ کاراکتر و شامل حروف لاتین، ارقام، نقطه یا خط زیر باشد.")

    class InvalidPasswordException(val reason: String = "رمز عبور باید حداقل ۶ کاراکتر باشد.") : 
        RegistrationException(reason)

    class UnauthorizedAdminCreationException : 
        RegistrationException("فقط مدیران ارشد سیستم مجاز به ایجاد کاربر مدیر جدید هستند.")
    
    class DatabaseConstraintException(message: String) : 
        RegistrationException(message)

    class InvalidCredentialsException :
        RegistrationException("نام کاربری/شماره تلفن یا رمز عبور نادرست است.")

    class AccountNotApprovedException(val status: AccountApprovalStatus) :
        RegistrationException(
            if (status == AccountApprovalStatus.REJECTED)
                "درخواست عضویت این حساب کاربری توسط مدیر سیستم رد شده است."
            else
                "حساب کاربری شما در انتظار تایید مدیر سیستم می‌باشد."
        )
}
