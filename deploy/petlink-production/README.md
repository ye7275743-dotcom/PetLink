# PetLink same-IP production stack

This stack deliberately leaves the existing project on host port 80 untouched.
PetLink is exposed on host port 8081:

- PC: `http://103.233.254.186:8081/`
- Mobile H5: `http://103.233.254.186:8081/mobile/`
- API: `http://103.233.254.186:8081/api/`

访问根路径时由 User-Agent 自动选择端：桌面浏览器进入 PC，手机浏览器进入
Mobile H5。桌面浏览器直接打开旧的 `/mobile/` 地址会自动回到 `/`，因此不会把
移动端布局横向拉伸到电脑屏幕。移动端个人中心的“切换到电脑版”会保留显式的
PC 偏好；旧的 `petlink_view=mobile` Cookie 不会覆盖桌面设备识别。

The backend listens only inside the Compose network on port 8080, and MySQL is
persisted in the `petlink_mysql_data` Docker volume. Copy the local PC `dist/`
and Mobile `dist/build/h5/` outputs into `web/pc/` and `web/mobile/` before
building the web image. Mobile H5 must be built with `PETLINK_H5_BASE=/mobile/`.

Before first start, copy `.env.example` to `.env`, set unique production
secrets, initialize `sql/petlink.sql`, and create production ADMIN/RESCUER
accounts with BCrypt hashes. Do not use local development seed credentials.
