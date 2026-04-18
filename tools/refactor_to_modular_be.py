#!/usr/bin/env python3
"""One-shot refactor: src/main/java/com/fcar -> be/src/main/java with core + modules layout."""
from __future__ import annotations

import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC_JAVA = ROOT / "src" / "main" / "java"
BE_JAVA = ROOT / "be" / "src" / "main" / "java"

# old_path relative to SRC_JAVA -> new_path relative to BE_JAVA
MOVES: dict[str, str] = {}

def add(old: str, new: str) -> None:
    MOVES[old.replace("\\", "/")] = new.replace("\\", "/")


# --- core ---
add("com/fcar/domain/BaseEntity.java", "com/fcar/core/entity/BaseEntity.java")
for name in (
    "BadRequestException",
    "ErrorResponse",
    "GlobalExceptionHandler",
    "NotFoundException",
    "ValidationException",
):
    add(f"com/fcar/exception/{name}.java", f"com/fcar/core/exception/{name}.java")
add("com/fcar/util/VietnamPhoneRules.java", "com/fcar/core/util/VietnamPhoneRules.java")
add("com/fcar/config/PasswordConfig.java", "com/fcar/core/config/PasswordConfig.java")
add("com/fcar/config/SecurityConfig.java", "com/fcar/core/config/SecurityConfig.java")
add("com/fcar/service/MailNotificationService.java", "com/fcar/core/email/MailNotificationService.java")
add("com/fcar/service/email/CustomerEmailHtmlShell.java", "com/fcar/core/email/CustomerEmailHtmlShell.java")

# --- application root ---
add("com/fcar/FcarApplication.java", "com/fcar/FcarApplication.java")

# --- user ---
for n in ("User", "UserPhone", "OtpToken"):
    add(f"com/fcar/domain/{n}.java", f"com/fcar/modules/user/entity/{n}.java")
for n in ("UserRole", "OtpType"):
    add(f"com/fcar/domain/enums/{n}.java", f"com/fcar/modules/user/entity/enums/{n}.java")
for n in ("UserRepository", "UserPhoneRepository", "OtpTokenRepository"):
    add(f"com/fcar/repository/{n}.java", f"com/fcar/modules/user/repository/{n}.java")
for n in (
    "AuthController",
    "AccountController",
    "AccountPhoneModalApiController",
    "AccountProfileApiController",
    "AccountProfileController",
    "ForgotPasswordController",
    "OtpAuthController",
):
    add(f"com/fcar/controller/{n}.java", f"com/fcar/modules/user/controller/{n}.java")
add("com/fcar/controller/admin/AdminUserController.java", "com/fcar/modules/user/controller/admin/AdminUserController.java")
for n in ("UserService", "OtpService", "SessionService"):
    add(f"com/fcar/service/{n}.java", f"com/fcar/modules/user/service/{n}.java")
for n in (
    "AuthenticatedUserResolver",
    "FcarUserDetails",
    "FcarUserDetailsService",
    "GoogleOAuth2SuccessHandler",
    "LoginSuccessHandler",
    "PostLoginRedirectService",
):
    add(f"com/fcar/security/{n}.java", f"com/fcar/modules/user/security/{n}.java")
for n in ("DataInitializer", "GlobalModelAttributes", "PhoneNumberRequiredInterceptor", "WebConfig"):
    add(f"com/fcar/config/{n}.java", f"com/fcar/modules/user/config/{n}.java")

# --- catalog ---
for n in (
    "Brand",
    "CarModel",
    "CarDefinition",
    "CarInventory",
    "CarAttribute",
    "CarColor",
    "CarImage",
    "Segment",
    "CarImportHistory",
):
    add(f"com/fcar/domain/{n}.java", f"com/fcar/modules/catalog/entity/{n}.java")
add("com/fcar/domain/enums/BodyType.java", "com/fcar/modules/catalog/entity/enums/BodyType.java")
for n in (
    "BrandRepository",
    "CarModelRepository",
    "CarDefinitionRepository",
    "CarInventoryRepository",
    "CarAttributeRepository",
    "CarColorRepository",
    "CarImageRepository",
    "SegmentRepository",
    "CarImportHistoryRepository",
):
    add(f"com/fcar/repository/{n}.java", f"com/fcar/modules/catalog/repository/{n}.java")
for n in (
    "HomeController",
    "CarController",
    "CarCompareController",
    "StorefrontCarFragmentController",
    "StorefrontCarListingModelHelper",
):
    add(f"com/fcar/controller/{n}.java", f"com/fcar/modules/catalog/controller/{n}.java")
for n in (
    "AdminBrandController",
    "AdminCarModelController",
    "AdminCarDefinitionController",
    "AdminCarInventoryController",
    "AdminSegmentController",
):
    add(f"com/fcar/controller/admin/{n}.java", f"com/fcar/modules/catalog/controller/admin/{n}.java")
for n in ("CarQueryService", "CatalogActiveCascadeService", "CarInventoryMergeService"):
    add(f"com/fcar/service/{n}.java", f"com/fcar/modules/catalog/service/{n}.java")
add(
    "com/fcar/service/display/CarDefinitionLabelFormatter.java",
    "com/fcar/modules/catalog/service/display/CarDefinitionLabelFormatter.java",
)
for n in ("StorefrontCarDefinitionSpecification", "StorefrontFilterOptions", "StorefrontListingFilter"):
    add(f"com/fcar/service/storefront/{n}.java", f"com/fcar/modules/catalog/service/storefront/{n}.java")

# --- order ---
add("com/fcar/domain/CarOrder.java", "com/fcar/modules/order/entity/CarOrder.java")
add("com/fcar/domain/enums/OrderStatus.java", "com/fcar/modules/order/entity/enums/OrderStatus.java")
add("com/fcar/repository/CarOrderRepository.java", "com/fcar/modules/order/repository/CarOrderRepository.java")
add("com/fcar/controller/OrderController.java", "com/fcar/modules/order/controller/OrderController.java")
add("com/fcar/controller/admin/AdminOrderController.java", "com/fcar/modules/order/controller/admin/AdminOrderController.java")
for n in ("CarOrderDepositService", "CarOrderRefundRules", "OrderEmailHtmlService"):
    add(f"com/fcar/service/{n}.java", f"com/fcar/modules/order/service/{n}.java")
add(
    "com/fcar/service/display/OrderStatusLabelFormatter.java",
    "com/fcar/modules/order/service/display/OrderStatusLabelFormatter.java",
)

# --- payment ---
for n in ("PayosDepositSession", "PaymentConfig", "StoreBankAccount"):
    add(f"com/fcar/domain/{n}.java", f"com/fcar/modules/payment/entity/{n}.java")
add(
    "com/fcar/domain/enums/PayosDepositSessionStatus.java",
    "com/fcar/modules/payment/entity/enums/PayosDepositSessionStatus.java",
)
for n in ("PayosDepositSessionRepository", "PaymentConfigRepository", "StoreBankAccountRepository"):
    add(f"com/fcar/repository/{n}.java", f"com/fcar/modules/payment/repository/{n}.java")
add("com/fcar/controller/PayOsWebhookController.java", "com/fcar/modules/payment/controller/PayOsWebhookController.java")
add("com/fcar/controller/admin/AdminPaymentController.java", "com/fcar/modules/payment/controller/admin/AdminPaymentController.java")
for n in ("PayOsDepositService", "PayOsSignatureService"):
    add(f"com/fcar/service/payos/{n}.java", f"com/fcar/modules/payment/service/payos/{n}.java")
add("com/fcar/service/VietQrImageService.java", "com/fcar/modules/payment/service/VietQrImageService.java")
add("com/fcar/config/PayOsConfig.java", "com/fcar/modules/payment/config/PayOsConfig.java")
add("com/fcar/config/FcarPayOsProperties.java", "com/fcar/modules/payment/config/FcarPayOsProperties.java")

# --- contact ---
add("com/fcar/domain/ContactRequest.java", "com/fcar/modules/contact/entity/ContactRequest.java")
add("com/fcar/domain/enums/ContactStatus.java", "com/fcar/modules/contact/entity/enums/ContactStatus.java")
add("com/fcar/repository/ContactRequestRepository.java", "com/fcar/modules/contact/repository/ContactRequestRepository.java")
add("com/fcar/controller/ContactController.java", "com/fcar/modules/contact/controller/ContactController.java")
add(
    "com/fcar/controller/admin/AdminContactRequestController.java",
    "com/fcar/modules/contact/controller/admin/AdminContactRequestController.java",
)

# --- testdrive ---
add("com/fcar/domain/TestDriveBooking.java", "com/fcar/modules/testdrive/entity/TestDriveBooking.java")
add("com/fcar/domain/enums/TestDriveStatus.java", "com/fcar/modules/testdrive/entity/enums/TestDriveStatus.java")
add("com/fcar/repository/TestDriveBookingRepository.java", "com/fcar/modules/testdrive/repository/TestDriveBookingRepository.java")
add("com/fcar/controller/TestDriveController.java", "com/fcar/modules/testdrive/controller/TestDriveController.java")
add("com/fcar/controller/admin/AdminTestDriveController.java", "com/fcar/modules/testdrive/controller/admin/AdminTestDriveController.java")
add(
    "com/fcar/service/ContactTestDriveEmailHtmlService.java",
    "com/fcar/modules/testdrive/service/ContactTestDriveEmailHtmlService.java",
)

# --- review ---
add("com/fcar/domain/CarReview.java", "com/fcar/modules/review/entity/CarReview.java")
add("com/fcar/repository/CarReviewRepository.java", "com/fcar/modules/review/repository/CarReviewRepository.java")
add("com/fcar/controller/CarReviewController.java", "com/fcar/modules/review/controller/CarReviewController.java")
add("com/fcar/controller/admin/AdminCarReviewController.java", "com/fcar/modules/review/controller/admin/AdminCarReviewController.java")

# --- favorite ---
add("com/fcar/domain/Favorite.java", "com/fcar/modules/favorite/entity/Favorite.java")
add("com/fcar/repository/FavoriteRepository.java", "com/fcar/modules/favorite/repository/FavoriteRepository.java")
add("com/fcar/controller/FavoriteController.java", "com/fcar/modules/favorite/controller/FavoriteController.java")
add("com/fcar/controller/FavoriteApiController.java", "com/fcar/modules/favorite/controller/FavoriteApiController.java")
add("com/fcar/service/FavoriteService.java", "com/fcar/modules/favorite/service/FavoriteService.java")

# --- branch ---
for n in ("Branch", "SupportHotline"):
    add(f"com/fcar/domain/{n}.java", f"com/fcar/modules/branch/entity/{n}.java")
add("com/fcar/domain/enums/BranchStatus.java", "com/fcar/modules/branch/entity/enums/BranchStatus.java")
for n in ("BranchRepository", "SupportHotlineRepository"):
    add(f"com/fcar/repository/{n}.java", f"com/fcar/modules/branch/repository/{n}.java")
add("com/fcar/controller/admin/AdminBranchController.java", "com/fcar/modules/branch/controller/admin/AdminBranchController.java")

# --- report ---
add("com/fcar/service/report/ReportService.java", "com/fcar/modules/report/service/ReportService.java")
add("com/fcar/service/report/ReportDashboardModel.java", "com/fcar/modules/report/service/ReportDashboardModel.java")
add("com/fcar/controller/admin/AdminReportController.java", "com/fcar/modules/report/controller/admin/AdminReportController.java")
add("com/fcar/controller/admin/AdminDashboardController.java", "com/fcar/modules/report/controller/admin/AdminDashboardController.java")


def replace_imports(text: str) -> str:
    # Order: longer / more specific patterns first
    pairs: list[tuple[str, str]] = [
        ("com.fcar.service.storefront.", "com.fcar.modules.catalog.service.storefront."),
        ("com.fcar.service.payos.", "com.fcar.modules.payment.service.payos."),
        ("com.fcar.service.report.", "com.fcar.modules.report.service."),
        ("com.fcar.service.display.CarDefinitionLabelFormatter", "com.fcar.modules.catalog.service.display.CarDefinitionLabelFormatter"),
        ("com.fcar.service.display.OrderStatusLabelFormatter", "com.fcar.modules.order.service.display.OrderStatusLabelFormatter"),
        ("com.fcar.service.email.CustomerEmailHtmlShell", "com.fcar.core.email.CustomerEmailHtmlShell"),
        ("com.fcar.service.ContactTestDriveEmailHtmlService", "com.fcar.modules.testdrive.service.ContactTestDriveEmailHtmlService"),
        ("com.fcar.domain.enums.PayosDepositSessionStatus", "com.fcar.modules.payment.entity.enums.PayosDepositSessionStatus"),
        ("com.fcar.domain.enums.TestDriveStatus", "com.fcar.modules.testdrive.entity.enums.TestDriveStatus"),
        ("com.fcar.domain.enums.ContactStatus", "com.fcar.modules.contact.entity.enums.ContactStatus"),
        ("com.fcar.domain.enums.OrderStatus", "com.fcar.modules.order.entity.enums.OrderStatus"),
        ("com.fcar.domain.enums.BranchStatus", "com.fcar.modules.branch.entity.enums.BranchStatus"),
        ("com.fcar.domain.enums.BodyType", "com.fcar.modules.catalog.entity.enums.BodyType"),
        ("com.fcar.domain.enums.UserRole", "com.fcar.modules.user.entity.enums.UserRole"),
        ("com.fcar.domain.enums.OtpType", "com.fcar.modules.user.entity.enums.OtpType"),
        ("com.fcar.domain.PayosDepositSession", "com.fcar.modules.payment.entity.PayosDepositSession"),
        ("com.fcar.domain.PaymentConfig", "com.fcar.modules.payment.entity.PaymentConfig"),
        ("com.fcar.domain.StoreBankAccount", "com.fcar.modules.payment.entity.StoreBankAccount"),
        ("com.fcar.domain.ContactRequest", "com.fcar.modules.contact.entity.ContactRequest"),
        ("com.fcar.domain.TestDriveBooking", "com.fcar.modules.testdrive.entity.TestDriveBooking"),
        ("com.fcar.domain.CarReview", "com.fcar.modules.review.entity.CarReview"),
        ("com.fcar.domain.Favorite", "com.fcar.modules.favorite.entity.Favorite"),
        ("com.fcar.domain.CarOrder", "com.fcar.modules.order.entity.CarOrder"),
        ("com.fcar.domain.CarImportHistory", "com.fcar.modules.catalog.entity.CarImportHistory"),
        ("com.fcar.domain.CarDefinition", "com.fcar.modules.catalog.entity.CarDefinition"),
        ("com.fcar.domain.CarInventory", "com.fcar.modules.catalog.entity.CarInventory"),
        ("com.fcar.domain.CarAttribute", "com.fcar.modules.catalog.entity.CarAttribute"),
        ("com.fcar.domain.CarColor", "com.fcar.modules.catalog.entity.CarColor"),
        ("com.fcar.domain.CarImage", "com.fcar.modules.catalog.entity.CarImage"),
        ("com.fcar.domain.CarModel", "com.fcar.modules.catalog.entity.CarModel"),
        ("com.fcar.domain.Segment", "com.fcar.modules.catalog.entity.Segment"),
        ("com.fcar.domain.Brand", "com.fcar.modules.catalog.entity.Brand"),
        ("com.fcar.domain.Branch", "com.fcar.modules.branch.entity.Branch"),
        ("com.fcar.domain.SupportHotline", "com.fcar.modules.branch.entity.SupportHotline"),
        ("com.fcar.domain.UserPhone", "com.fcar.modules.user.entity.UserPhone"),
        ("com.fcar.domain.OtpToken", "com.fcar.modules.user.entity.OtpToken"),
        ("com.fcar.domain.User", "com.fcar.modules.user.entity.User"),
        ("com.fcar.domain.BaseEntity", "com.fcar.core.entity.BaseEntity"),
        ("com.fcar.repository.PayosDepositSessionRepository", "com.fcar.modules.payment.repository.PayosDepositSessionRepository"),
        ("com.fcar.repository.PaymentConfigRepository", "com.fcar.modules.payment.repository.PaymentConfigRepository"),
        ("com.fcar.repository.StoreBankAccountRepository", "com.fcar.modules.payment.repository.StoreBankAccountRepository"),
        ("com.fcar.repository.ContactRequestRepository", "com.fcar.modules.contact.repository.ContactRequestRepository"),
        ("com.fcar.repository.TestDriveBookingRepository", "com.fcar.modules.testdrive.repository.TestDriveBookingRepository"),
        ("com.fcar.repository.CarReviewRepository", "com.fcar.modules.review.repository.CarReviewRepository"),
        ("com.fcar.repository.FavoriteRepository", "com.fcar.modules.favorite.repository.FavoriteRepository"),
        ("com.fcar.repository.CarOrderRepository", "com.fcar.modules.order.repository.CarOrderRepository"),
        ("com.fcar.repository.CarImportHistoryRepository", "com.fcar.modules.catalog.repository.CarImportHistoryRepository"),
        ("com.fcar.repository.CarDefinitionRepository", "com.fcar.modules.catalog.repository.CarDefinitionRepository"),
        ("com.fcar.repository.CarInventoryRepository", "com.fcar.modules.catalog.repository.CarInventoryRepository"),
        ("com.fcar.repository.CarAttributeRepository", "com.fcar.modules.catalog.repository.CarAttributeRepository"),
        ("com.fcar.repository.CarColorRepository", "com.fcar.modules.catalog.repository.CarColorRepository"),
        ("com.fcar.repository.CarImageRepository", "com.fcar.modules.catalog.repository.CarImageRepository"),
        ("com.fcar.repository.CarModelRepository", "com.fcar.modules.catalog.repository.CarModelRepository"),
        ("com.fcar.repository.SegmentRepository", "com.fcar.modules.catalog.repository.SegmentRepository"),
        ("com.fcar.repository.BrandRepository", "com.fcar.modules.catalog.repository.BrandRepository"),
        ("com.fcar.repository.BranchRepository", "com.fcar.modules.branch.repository.BranchRepository"),
        ("com.fcar.repository.SupportHotlineRepository", "com.fcar.modules.branch.repository.SupportHotlineRepository"),
        ("com.fcar.repository.UserPhoneRepository", "com.fcar.modules.user.repository.UserPhoneRepository"),
        ("com.fcar.repository.OtpTokenRepository", "com.fcar.modules.user.repository.OtpTokenRepository"),
        ("com.fcar.repository.UserRepository", "com.fcar.modules.user.repository.UserRepository"),
        ("com.fcar.security.AuthenticatedUserResolver", "com.fcar.modules.user.security.AuthenticatedUserResolver"),
        ("com.fcar.security.PostLoginRedirectService", "com.fcar.modules.user.security.PostLoginRedirectService"),
        ("com.fcar.security.FcarUserDetailsService", "com.fcar.modules.user.security.FcarUserDetailsService"),
        ("com.fcar.security.GoogleOAuth2SuccessHandler", "com.fcar.modules.user.security.GoogleOAuth2SuccessHandler"),
        ("com.fcar.security.LoginSuccessHandler", "com.fcar.modules.user.security.LoginSuccessHandler"),
        ("com.fcar.security.FcarUserDetails", "com.fcar.modules.user.security.FcarUserDetails"),
        ("com.fcar.exception.", "com.fcar.core.exception."),
        ("com.fcar.util.VietnamPhoneRules", "com.fcar.core.util.VietnamPhoneRules"),
        ("com.fcar.config.FcarPayOsProperties", "com.fcar.modules.payment.config.FcarPayOsProperties"),
        ("com.fcar.config.PayOsConfig", "com.fcar.modules.payment.config.PayOsConfig"),
        ("com.fcar.service.VietQrImageService", "com.fcar.modules.payment.service.VietQrImageService"),
        ("com.fcar.service.MailNotificationService", "com.fcar.core.email.MailNotificationService"),
        ("com.fcar.service.FavoriteService", "com.fcar.modules.favorite.service.FavoriteService"),
        ("com.fcar.service.OrderEmailHtmlService", "com.fcar.modules.order.service.OrderEmailHtmlService"),
        ("com.fcar.service.CarOrderRefundRules", "com.fcar.modules.order.service.CarOrderRefundRules"),
        ("com.fcar.service.CarOrderDepositService", "com.fcar.modules.order.service.CarOrderDepositService"),
        ("com.fcar.service.CatalogActiveCascadeService", "com.fcar.modules.catalog.service.CatalogActiveCascadeService"),
        ("com.fcar.service.CarInventoryMergeService", "com.fcar.modules.catalog.service.CarInventoryMergeService"),
        ("com.fcar.service.CarQueryService", "com.fcar.modules.catalog.service.CarQueryService"),
        ("com.fcar.service.UserService", "com.fcar.modules.user.service.UserService"),
        ("com.fcar.service.OtpService", "com.fcar.modules.user.service.OtpService"),
        ("com.fcar.service.SessionService", "com.fcar.modules.user.service.SessionService"),
    ]
    pairs.sort(key=lambda x: len(x[0]), reverse=True)
    for old, new in pairs:
        text = text.replace(old, new)
    return text


def package_from_path(rel: str) -> str:
    p = Path(rel)
    return ".".join(p.parent.parts)


def main() -> None:
    if not SRC_JAVA.is_dir():
        raise SystemExit(f"Missing {SRC_JAVA}")

    BE_JAVA.mkdir(parents=True, exist_ok=True)

    for old_rel, new_rel in MOVES.items():
        old_path = SRC_JAVA / old_rel
        new_path = BE_JAVA / new_rel
        if not old_path.is_file():
            raise SystemExit(f"Missing source file: {old_path}")
        new_path.parent.mkdir(parents=True, exist_ok=True)
        text = old_path.read_text(encoding="utf-8")
        text = replace_imports(text)
        # package line
        lines = text.splitlines(keepends=True)
        if lines and lines[0].startswith("package "):
            lines[0] = f"package {package_from_path(Path(new_rel).as_posix())};\n"
            text = "".join(lines)
        new_path.write_text(text, encoding="utf-8")

    # resources
    res_src = ROOT / "src" / "main" / "resources"
    res_dst = ROOT / "be" / "src" / "main" / "resources"
    if res_src.is_dir():
        res_dst.parent.mkdir(parents=True, exist_ok=True)
        if res_dst.exists():
            shutil.rmtree(res_dst)
        shutil.copytree(res_src, res_dst)

    # remove legacy src tree (resources already copied under be/)
    legacy_src = ROOT / "src"
    if legacy_src.is_dir():
        shutil.rmtree(legacy_src)

    print(f"Wrote {len(MOVES)} Java files under {BE_JAVA}")
    print(f"Copied resources to {res_dst}")
    print(f"Removed {legacy_src}")


if __name__ == "__main__":
    main()
