# PetLink same-IP production stack

This stack deliberately leaves the existing project on host port 80 untouched.
PetLink is exposed on host port 8081:

- PC: `http://103.233.254.186:8081/`
- Mobile H5: `http://103.233.254.186:8081/mobile/`
- API: `http://103.233.254.186:8081/api/`

The backend listens only inside the Compose network on port 8080, and MySQL is
persisted in the `petlink_mysql_data` Docker volume. Copy the local PC `dist/`
and Mobile `dist/build/h5/` outputs into `web/pc/` and `web/mobile/` before
building the web image. Mobile H5 must be built with `PETLINK_H5_BASE=/mobile/`.

Before first start, copy `.env.example` to `.env`, set unique production
secrets, initialize `sql/petlink.sql`, and create production ADMIN/RESCUER
accounts with BCrypt hashes. Do not use local development seed credentials.
