package com.example.plugins

import com.example.db.DatabaseConnection
import com.example.db.entities.*
import com.example.model.Response
import com.example.model.UserCredential
import com.example.model.UserLogin
import com.example.model.admin.AdminReq
import com.example.model.siswa.Alternatif2Req
import com.example.model.siswa.AlternatifReq
import com.example.model.siswa.UpdateDataSiswaRequest
import com.example.utils.TokenManager
import com.typesafe.config.ConfigFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ktorm.dsl.*
import org.mindrot.jbcrypt.BCrypt


fun Application.configureRouting() {
    val db = DatabaseConnection.database
    val tokenManager = TokenManager(HoconApplicationConfig(ConfigFactory.load()))

    routing {

        // R E G I S T E R
        post("/register") {
            val userCred = call.receive<UserCredential>()

            if (!userCred.isValidCredentials()) {
                call.respond(
                    HttpStatusCode.BadRequest, Response(
                        success = false,
                        data = "username harus lebih dari 3 karakter atau password harus lebih dari 8 karakter"
                    )
                )
                return@post
            }

            val username = userCred.username.toLowerCase()
            val password = userCred.hashedPassword()
            val sebagai = userCred.sebagai.toLowerCase()

            // check if username already exist
            val user = db.from(UserRegistEntity)
                .select()
                .where { UserRegistEntity.username eq username }
                .map {
                    it[UserRegistEntity.username]
                }
                .firstOrNull()

            if (user != null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    Response(success = false, data = "username telah digunakan, silahkan gunakan username yang lain")
                )
                return@post
            }
            db.insert(UserRegistEntity) {
                set(it.username, username)
                set(it.password, password)
                set(it.sebagai, sebagai)
            }
            call.respond(
                HttpStatusCode.Created,
                Response(success = true, data = "User has been succesfully created")
            )

        }

        // L O G I N
        post("/login") {
            val userCred = call.receive<UserCredential>()
            if (!userCred.isValidCredentials()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    Response(
                        success = false,
                        data = "User should be greater than or equal to 3 and password has ben equal 8"
                    )
                )
                return@post
            }
            val username = userCred.username.toLowerCase()
            val password = userCred.password
            val sebagai = userCred.sebagai.toLowerCase()

            //check if user exist

            val user = db.from(UserRegistEntity)
                .select()
                .where { UserRegistEntity.username eq username }
                .map {
                    val id = it[UserRegistEntity.id]!!
                    val userName = it[UserRegistEntity.username]!!
                    val passWord = it[UserRegistEntity.password]!!
                    val sebaGai = it[UserRegistEntity.sebagai]!!
                    UserLogin(id, userName, passWord, sebaGai)
                }.firstOrNull()

            if (user == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    Response(success = false, data = "invalid username or password")
                )
                return@post
            }
            val doesPasswordMatch = BCrypt.checkpw(password, user.password)
            if (!doesPasswordMatch) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    Response(success = false, data = "invalid username or password")
                )
                return@post
            }
            val token = tokenManager.generateJWTToken(user)
            call.respond(HttpStatusCode.OK, Response(success = true, data = token))


        }

        authenticate {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val id = principal!!.payload.getClaim("id").asInt()
                val username = principal!!.payload.getClaim("username").asString()
                val sebagai = principal!!.payload.getClaim("sebagai").asString()
                call.respondText("Hello , $username, With id : $id, as $sebagai")
            }

            // G E T  D A T A  D I R I  S I S W A
            get("/datadirisiswa/{nisn}") {
                val nisn = call.parameters["nisn"]?.toInt() ?: -1
                val user = db.from(UserEntity)
                    .select()
                    .where { UserEntity.nisn eq nisn }
                    .map {
                        val nisn = it[UserEntity.nisn]!!
                        val nama = it[UserEntity.nama]!!
                        val jk = it[UserEntity.jeniskelamin]!!
                        val agama = it[UserEntity.agama]!!
                        val kelas = it[UserEntity.kelas]!!
                        val alamat = it[UserEntity.alamat]!!
                        UpdateDataSiswaRequest(
                            nisn = nisn,
                            nama = nama,
                            jeniskelamin = jk,
                            agama = agama,
                            kelas = kelas,
                            alamat = alamat
                        )

                    }
                    .firstOrNull()
                if (user == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        Response(
                            success = false,
                            data = "could not found nisn = ${nisn}"
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.OK,
                        Response(
                            success = true,
                            data = user
                        )
                    )

                }


            }
            //  I N P U T  D A T A  D I R I  S I S W A
            post("/datadirisiswa") {
                val req = call.receive<UpdateDataSiswaRequest>()
                val nama = req.nama.toLowerCase()
                val check = db.from(UserEntity)
                    .select()
                    .where { UserEntity.nama eq nama }
                    .map {
                        it[UserEntity.nama]
                    }.firstOrNull()


                if (check != null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        Response(success = false, data = " nama sudah pernah dipakai atau gunakan nama lain")
                    )
                    return@post
                }
                db.insert(UserEntity) {
                    set(it.nisn, req.nisn)
                    set(it.nama, req.nama)
                    set(it.jeniskelamin, req.jeniskelamin)
                    set(it.agama, req.agama)
                    set(it.kelas, req.kelas)
                    set(it.alamat, req.alamat)
                }
                call.respond(
                    HttpStatusCode.Created,
                    Response(success = true, data = "User has been succesfully update")
                )
            }
            //  U P D A T E  D A T A  D I R I  S I S W A
            put("/datadirisiswa/{nisn}") {
                val nisn = call.parameters["nisn"]?.toInt() ?: -1
                val req = call.receive<UpdateDataSiswaRequest>()
                val user = db.from(UserEntity)
                    .select()
                    .where { UserEntity.nisn eq nisn }
                    .map {
                        it[UserEntity.nisn]
                    }
                    .firstOrNull()

                if (user == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    db.update(UserEntity) {
                        where { UserEntity.nisn eq nisn }
                        set(it.nisn, req.nisn)
                        set(it.nama, req.nama)
                        set(it.jeniskelamin, req.jeniskelamin)
                        set(it.agama, req.agama)
                        set(it.kelas, req.kelas)
                        set(it.alamat, req.alamat)
                    }
                    call.respond(Response(success = true, data = "User has been succesfully updated"))
                }
            }
            // D E L E T E  D A T A  D I R I  S I S W A
            delete("/datadirisiswa/{nisn}") {
                val nisn = call.parameters["nisn"]
                val nisnToInt = nisn?.toInt() ?: -1
                val rowsDeleted = db.delete(UserEntity) { UserEntity.nisn eq nisnToInt }
                if (rowsDeleted == 0) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(Response(success = true, data = "User has been succesfully deleted"))
                }
            }
            // P O S T  A L T E R N A T I F
            post("/alternatif") {
                val req = call.receive<AlternatifReq>()
                val nisn = req.nisn
                val check = db.from(UserEntity)
                    .select()
                    .where { UserEntity.nisn eq nisn }
                    .map {
                        it[UserEntity.nisn]
                    }.firstOrNull()


                if (check == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        Response(success = false, data = " nisn tidak sesuai ")
                    )
                    return@post
                }

                db.insert(AlternatifEntity) {
                    set(it.nisn, req.nisn)
                    set(it.id, req.id)
                    set(it.nama, req.nama)
                    set(it.pend_agama, req.pend_agama)
                    set(it.pend_pancasila, req.pend_pancasila)
                    set(it.bahasa_indo, req.bahasa_indo)
                    set(it.mtk, req.mtk)
                    set(it.sejarah_indo, req.sejarah_indo)
                    set(it.bahasa_ing, req.bahasa_ing)
                    set(it.seni_budaya, req.seni_budaya)
                    set(it.penjas, req.penjas)
                    set(it.prakarya_dan_kwh, req.prakarya_dan_kwh)
                    set(it.biologi, req.biologi)
                    set(it.fisika, req.fisika)
                    set(it.kimia, req.kimia)
                    set(it.sosiologi, req.sosiologi)
                    set(it.ekonomi, req.ekonomi)
                    set(it.ket_pend_agama, req.ket_pend_agama)
                    set(it.ket_pend_pancasila, req.ket_pend_pancasila)
                    set(it.ket_bahasa_indo, req.ket_bahasa_indo)
                    set(it.ket_matematika, req.ket_matematika)
                    set(it.ket_sejarah_indo, req.ket_sejarah_indo)
                    set(it.ket_bahasa_ing, req.ket_bahasa_ing)
                    set(it.ket_seni_budaya, req.ket_seni_budaya)
                    set(it.ket_penjas, req.ket_penjas)
                    set(it.ket_prakarya_dan_kwh, req.ket_prakarya_dan_kwh)
                    set(it.ket_biologi, req.ket_biologi)
                    set(it.ket_fisika, req.ket_fisika)
                    set(it.ket_kimia, req.ket_kimia)
                    set(it.ket_sosiologi, req.ket_sosiologi)
                    set(it.ket_ekonomi, req.ket_ekonomi)
                    set(it.pramuka, req.pramuka)
                    set(it.olahraga, req.olahraga)
                    set(it.pmr, req.pmr)
                    set(it.sikap_spiritual, req.sikap_spiritual)
                    set(it.sikap_sosial, req.sikap_sosial)
                    set(it.sakit, req.sakit)
                    set(it.izin, req.izin)
                    set(it.tanpa_keterangan, req.tanpa_keterangan)
                }
                call.respond(
                    HttpStatusCode.Created,
                    Response(success = true, data = "kriteria succesfully post")
                )

            }

            // P O S T  A L T E R N A T I F 2
            post("/alternatif2") {
                val req = call.receive<Alternatif2Req>()
                val nisn = req.nisn
                val check = db.from(UserEntity)
                    .select()
                    .where { UserEntity.nisn eq nisn }
                    .map {
                        it[UserEntity.nisn]
                    }.firstOrNull()


                if (check == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        Response(success = false, data = " nisn tidak sesuai ")
                    )
                    return@post
                }

                db.insert(Alternatif2Entity) {
                    set(it.nisn, req.nisn)
                    set(it.id, req.id)
                    set(it.nama, req.nama)
                    set(it.pend_agama, req.pend_agama)
                    set(it.pend_pancasila, req.pend_pancasila)
                    set(it.bahasa_indo, req.bahasa_indo)
                    set(it.mtk, req.mtk)
                    set(it.sejarah_indo, req.sejarah_indo)
                    set(it.bahasa_ing, req.bahasa_ing)
                    set(it.seni_budaya, req.seni_budaya)
                    set(it.penjas, req.penjas)
                    set(it.prakarya_dan_kwh, req.prakarya_dan_kwh)
                    set(it.biologi, req.biologi)
                    set(it.fisika, req.fisika)
                    set(it.kimia, req.kimia)
                    set(it.sosiologi, req.sosiologi)
                    set(it.ekonomi, req.ekonomi)
                    set(it.ket_pend_agama, req.ket_pend_agama)
                    set(it.ket_pend_pancasila, req.ket_pend_pancasila)
                    set(it.ket_bahasa_indo, req.ket_bahasa_indo)
                    set(it.ket_matematika, req.ket_matematika)
                    set(it.ket_sejarah_indo, req.ket_sejarah_indo)
                    set(it.ket_bahasa_ing, req.ket_bahasa_ing)
                    set(it.ket_seni_budaya, req.ket_seni_budaya)
                    set(it.ket_penjas, req.ket_penjas)
                    set(it.ket_prakarya_dan_kwh, req.ket_prakarya_dan_kwh)
                    set(it.ket_biologi, req.ket_biologi)
                    set(it.ket_fisika, req.ket_fisika)
                    set(it.ket_kimia, req.ket_kimia)
                    set(it.ket_sosiologi, req.ket_sosiologi)
                    set(it.ket_ekonomi, req.ket_ekonomi)
                    set(it.pramuka, req.pramuka)
                    set(it.olahraga, req.olahraga)
                    set(it.pmr, req.pmr)
                    set(it.sikap_spiritual, req.sikap_spiritual)
                    set(it.sikap_sosial, req.sikap_sosial)
                    set(it.sakit, req.sakit)
                    set(it.izin, req.izin)
                    set(it.tanpa_keterangan, req.tanpa_keterangan)
                }
                call.respond(
                    HttpStatusCode.Created,
                    Response(success = true, data = "kriteria succesfully post")
                )

            }


            // G E T  D A T A  D I R I  A D M I N
            get("/datadiriadmin/{id}") {
                val id = call.parameters["id"]?.toInt() ?: -1
                val user = db.from(AdminEntity)
                    .select()
                    .where { AdminEntity.id eq id }
                    .map {
                        val id = it[AdminEntity.id]!!
                        val nama = it[AdminEntity.nama]!!
                        val nip = it[AdminEntity.nip]!!
                        val jabatan = it[AdminEntity.jabatan]!!
                        AdminReq(
                            id = id,
                            nama = nama,
                            nip = nip,
                            jabatan = jabatan
                        )

                    }
                    .firstOrNull()
                if (user == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        Response(
                            success = false,
                            data = "could not found nisn = ${id}"
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.OK,
                        Response(
                            success = true,
                            data = user
                        )
                    )

                }


            }
            //  I N P U T  D A T A  D I R I  A D M I N
            post("/datadiriadmin") {
                val req = call.receive<AdminReq>()
                val nama = req.nama.toLowerCase()
                val check = db.from(AdminEntity)
                    .select()
                    .where { AdminEntity.nama eq nama }
                    .map {
                        it[AdminEntity.nama]
                    }.firstOrNull()


                if (check != null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        Response(success = false, data = " nama sudah pernah dipakai atau gunakan nama lain")
                    )
                    return@post
                }
                db.insert(AdminEntity) {
                    set(it.nama, req.nama)
                    set(it.nip, req.nip)
                    set(it.jabatan, req.jabatan)
                }
                call.respond(
                    HttpStatusCode.Created,
                    Response(success = true, data = "User has been succesfully update")
                )
            }
            //  U P D A T E  D A T A  D I R I  S I S W A
            put("/datadiriadmin/{id}") {
                val id = call.parameters["id"]?.toInt() ?: -1
                val req = call.receive<AdminReq>()
                val user = db.from(AdminEntity)
                    .select()
                    .where { AdminEntity.id eq id }
                    .map {
                        it[AdminEntity.id]
                    }
                    .firstOrNull()

                if (user == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    db.update(AdminEntity) {
                        where {  AdminEntity.id eq id }
                        set(it.nama, req.nama)
                        set(it.nip, req.nip)
                        set(it.jabatan, req.jabatan)
                    }
                    call.respond(Response(success = true, data = "User has been succesfully updated"))
                }
            }
            // D E L E T E  D A T A  D I R I  S I S W A
            delete("/datadiriadmin/{id}") {
                val id = call.parameters["id"]?.toInt() ?: -1
                val rowsDeleted = db.delete(AdminEntity) { AdminEntity.id eq id }
                if (rowsDeleted == 0) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(Response(success = true, data = "User has been succesfully deleted"))
                }
            }
            // G E T  A L L  A L T E R N A T I F
            get("/alternatif") {
                val notes = db.from(AlternatifEntity).select()
                    .map {
                        val id = it[AlternatifEntity.id]
                        val nisn = it[AlternatifEntity.nisn]!!
                        val nama = it[AlternatifEntity.nama]!!
                        val pend_agama = it[AlternatifEntity.pend_agama]!!
                        val pend_pancasila = it[AlternatifEntity.pend_pancasila]!!
                        val bahasa_indo = it[AlternatifEntity.bahasa_indo]!!
                        val mtk = it[AlternatifEntity.mtk]!!
                        val sejarah_indo = it[AlternatifEntity.sejarah_indo]!!
                        val bahasa_ing = it[AlternatifEntity.bahasa_ing]!!
                        val seni_budaya = it[AlternatifEntity.seni_budaya]!!
                        val penjas = it[AlternatifEntity.penjas]!!
                        val prakarya_dan_kwh = it[AlternatifEntity.prakarya_dan_kwh]!!
                        val biologi = it[AlternatifEntity.biologi]!!
                        val fisika = it[AlternatifEntity.fisika]!!
                        val kimia = it[AlternatifEntity.kimia]!!
                        val sosiologi = it[AlternatifEntity.sosiologi]!!
                        val ekonomi = it[AlternatifEntity.ekonomi]!!
                        val ket_pend_agama = it[AlternatifEntity.ket_pend_agama]!!
                       val ket_pend_pancasila = it[AlternatifEntity.ket_pend_pancasila]!!
                        val ket_bahasa_indo = it[AlternatifEntity.ket_bahasa_indo]!!
                        val ket_matematika = it[AlternatifEntity.ket_matematika]!!
                        val ket_sejarah_indo = it[AlternatifEntity.ket_sejarah_indo]!!
                       val ket_bahasa_ing = it[AlternatifEntity.ket_bahasa_ing]!!
                        val ket_seni_budaya = it[AlternatifEntity.ket_seni_budaya]!!
                        val ket_penjas = it[AlternatifEntity.ket_penjas]!!
                        val ket_prakarya_dan_kwh = it[AlternatifEntity.ket_prakarya_dan_kwh]!!
                        val ket_biologi = it[AlternatifEntity.ket_biologi]!!
                        val ket_fisika = it[AlternatifEntity.ket_fisika]!!
                        val ket_kimia = it[AlternatifEntity.ket_kimia]!!
                        val ket_sosiologi = it[AlternatifEntity.ket_sosiologi]!!
                        val ket_ekonomi = it[AlternatifEntity.ket_ekonomi]!!
                        val pramuka = it[AlternatifEntity.pramuka]!!
                        val olahraga = it[AlternatifEntity.olahraga]!!
                        val pmr = it[AlternatifEntity.pmr]!!
                        val sikap_spiritual = it[AlternatifEntity.sikap_spiritual]!!
                        val sikap_sosial = it[AlternatifEntity.sikap_sosial]!!
                        val sakit = it[AlternatifEntity.sakit]!!
                        val izin = it[AlternatifEntity.izin]!!
                        val tanpa_keterangan = it[AlternatifEntity.tanpa_keterangan]!!

                        AlternatifReq(id ?: -1, nisn = nisn, nama = nama,
                            pend_agama = pend_agama,
                            pend_pancasila = pend_pancasila,
                            bahasa_indo = bahasa_indo,
                            mtk = mtk,
                            sejarah_indo = sejarah_indo,
                            bahasa_ing = bahasa_ing,
                            seni_budaya = seni_budaya,
                            penjas = penjas,
                            prakarya_dan_kwh = prakarya_dan_kwh,
                            biologi = biologi,
                            fisika = fisika,
                            kimia = kimia,
                            sosiologi = sosiologi,
                            ekonomi = ekonomi,
                            ket_pend_agama = ket_pend_agama,
                            ket_pend_pancasila = ket_pend_pancasila,
                            ket_bahasa_indo = ket_bahasa_indo,
                            ket_matematika = ket_matematika,
                            ket_sejarah_indo = ket_sejarah_indo,
                            ket_bahasa_ing = ket_bahasa_ing,
                            ket_seni_budaya = ket_seni_budaya,
                            ket_penjas = ket_penjas,
                            ket_prakarya_dan_kwh = ket_prakarya_dan_kwh,
                            ket_biologi = ket_biologi,
                            ket_fisika = ket_fisika,
                            ket_kimia = ket_kimia,
                            ket_sosiologi = ket_sosiologi,
                            ket_ekonomi = ket_ekonomi,
                            pramuka = pramuka,
                            olahraga = olahraga,
                            pmr = pmr,
                            sikap_spiritual = sikap_spiritual,
                            sikap_sosial = sikap_sosial,
                            sakit = sakit,
                            izin = izin,
                            tanpa_keterangan = tanpa_keterangan,
                            total = 0.0)
                    }
                call.respond(notes)
            }
            // A L T E R N A T I F  2
            get("/alternatif2") {
                val notes = db.from(Alternatif2Entity).select()
                    .map {
                        val id = it[Alternatif2Entity.id]
                        val nisn = it[Alternatif2Entity.nisn]!!
                        val nama = it[Alternatif2Entity.nama]!!
                        val pend_agama = it[Alternatif2Entity.pend_agama]!!
                        val pend_pancasila = it[Alternatif2Entity.pend_pancasila]!!
                        val bahasa_indo = it[Alternatif2Entity.bahasa_indo]!!
                        val mtk = it[Alternatif2Entity.mtk]!!
                        val sejarah_indo = it[Alternatif2Entity.sejarah_indo]!!
                        val bahasa_ing = it[Alternatif2Entity.bahasa_ing]!!
                        val seni_budaya = it[Alternatif2Entity.seni_budaya]!!
                        val penjas = it[Alternatif2Entity.penjas]!!
                        val prakarya_dan_kwh = it[Alternatif2Entity.prakarya_dan_kwh]!!
                        val biologi = it[Alternatif2Entity.biologi]!!
                        val fisika = it[Alternatif2Entity.fisika]!!
                        val kimia = it[Alternatif2Entity.kimia]!!
                        val sosiologi = it[Alternatif2Entity.sosiologi]!!
                        val ekonomi = it[Alternatif2Entity.ekonomi]!!
                        val ket_pend_agama = it[Alternatif2Entity.ket_pend_agama]!!
                        val ket_pend_pancasila = it[Alternatif2Entity.ket_pend_pancasila]!!
                        val ket_bahasa_indo = it[Alternatif2Entity.ket_bahasa_indo]!!
                        val ket_matematika = it[Alternatif2Entity.ket_matematika]!!
                        val ket_sejarah_indo = it[Alternatif2Entity.ket_sejarah_indo]!!
                        val ket_bahasa_ing = it[Alternatif2Entity.ket_bahasa_ing]!!
                        val ket_seni_budaya = it[Alternatif2Entity.ket_seni_budaya]!!
                        val ket_penjas = it[Alternatif2Entity.ket_penjas]!!
                        val ket_prakarya_dan_kwh = it[Alternatif2Entity.ket_prakarya_dan_kwh]!!
                        val ket_biologi = it[Alternatif2Entity.ket_biologi]!!
                        val ket_fisika = it[Alternatif2Entity.ket_fisika]!!
                        val ket_kimia = it[Alternatif2Entity.ket_kimia]!!
                        val ket_sosiologi = it[Alternatif2Entity.ket_sosiologi]!!
                        val ket_ekonomi = it[Alternatif2Entity.ket_ekonomi]!!
                        val pramuka = it[Alternatif2Entity.pramuka]!!
                        val olahraga = it[Alternatif2Entity.olahraga]!!
                        val pmr = it[Alternatif2Entity.pmr]!!
                        val sikap_spiritual = it[Alternatif2Entity.sikap_spiritual]!!
                        val sikap_sosial = it[Alternatif2Entity.sikap_sosial]!!
                        val sakit = it[Alternatif2Entity.sakit]!!
                        val izin = it[Alternatif2Entity.izin]!!
                        val tanpa_keterangan = it[Alternatif2Entity.tanpa_keterangan]!!

                        Alternatif2Req(id ?: -1, nisn = nisn, nama = nama,
                            pend_agama = pend_agama,
                            pend_pancasila = pend_pancasila,
                            bahasa_indo = bahasa_indo,
                            mtk = mtk,
                            sejarah_indo = sejarah_indo,
                            bahasa_ing = bahasa_ing,
                            seni_budaya = seni_budaya,
                            penjas = penjas,
                            prakarya_dan_kwh = prakarya_dan_kwh,
                            biologi = biologi,
                            fisika = fisika,
                            kimia = kimia,
                            sosiologi = sosiologi,
                            ekonomi = ekonomi,
                            ket_pend_agama = ket_pend_agama,
                            ket_pend_pancasila = ket_pend_pancasila,
                            ket_bahasa_indo = ket_bahasa_indo,
                            ket_matematika = ket_matematika,
                            ket_sejarah_indo = ket_sejarah_indo,
                            ket_bahasa_ing = ket_bahasa_ing,
                            ket_seni_budaya = ket_seni_budaya,
                            ket_penjas = ket_penjas,
                            ket_prakarya_dan_kwh = ket_prakarya_dan_kwh,
                            ket_biologi = ket_biologi,
                            ket_fisika = ket_fisika,
                            ket_kimia = ket_kimia,
                            ket_sosiologi = ket_sosiologi,
                            ket_ekonomi = ket_ekonomi,
                            pramuka = pramuka,
                            olahraga = olahraga,
                            pmr = pmr,
                            sikap_spiritual = sikap_spiritual,
                            sikap_sosial = sikap_sosial,
                            sakit = sakit,
                            izin = izin,
                            tanpa_keterangan = tanpa_keterangan,
                            total = 0.0)
                    }
                call.respond(notes)
            }
            // P E R A N G K I N G A N  A D M I N
            get("/admin/rangking") {


                // Mengambil data kriteria dari tabel
                val kriteriaList = db.from(AlternatifEntity).select().map { row ->
                    AlternatifReq(
                        id = row[AlternatifEntity.id]!!,
                        nisn = row[AlternatifEntity.nisn]!!,
                        nama = row[AlternatifEntity.nama]!!,
                        pend_agama = row[AlternatifEntity.pend_agama]!!,
                        pend_pancasila = row[AlternatifEntity.pend_pancasila]!!,
                        bahasa_indo = row[AlternatifEntity.bahasa_indo]!!,
                        mtk = row[AlternatifEntity.mtk]!!,
                        sejarah_indo = row[AlternatifEntity.sejarah_indo]!!,
                        bahasa_ing = row[AlternatifEntity.bahasa_ing]!!,
                        seni_budaya = row[AlternatifEntity.seni_budaya]!!,
                        penjas = row[AlternatifEntity.penjas]!!,
                        prakarya_dan_kwh = row[AlternatifEntity.prakarya_dan_kwh]!!,
                        biologi = row[AlternatifEntity.biologi]!!,
                        fisika = row[AlternatifEntity.fisika]!!,
                        kimia = row[AlternatifEntity.kimia]!!,
                        sosiologi = row[AlternatifEntity.sosiologi]!!,
                        ekonomi = row[AlternatifEntity.ekonomi]!!,
                        ket_pend_agama = row[AlternatifEntity.ket_pend_agama]!!,
                        ket_pend_pancasila = row[AlternatifEntity.ket_pend_pancasila]!!,
                        ket_bahasa_indo = row[AlternatifEntity.ket_bahasa_indo]!!,
                        ket_matematika = row[AlternatifEntity.ket_matematika]!!,
                        ket_sejarah_indo = row[AlternatifEntity.ket_sejarah_indo]!!,
                        ket_bahasa_ing = row[AlternatifEntity.ket_bahasa_ing]!!,
                        ket_seni_budaya = row[AlternatifEntity.ket_seni_budaya]!!,
                        ket_penjas = row[AlternatifEntity.ket_penjas]!!,
                        ket_prakarya_dan_kwh = row[AlternatifEntity.ket_prakarya_dan_kwh]!!,
                        ket_biologi = row[AlternatifEntity.ket_biologi]!!,
                        ket_fisika = row[AlternatifEntity.ket_fisika]!!,
                        ket_kimia = row[AlternatifEntity.ket_kimia]!!,
                        ket_sosiologi = row[AlternatifEntity.ket_sosiologi]!!,
                        ket_ekonomi = row[AlternatifEntity.ket_ekonomi]!!,
                        pramuka = row[AlternatifEntity.pramuka]!!,
                        olahraga = row[AlternatifEntity.olahraga]!!,
                        pmr = row[AlternatifEntity.pmr]!!,
                        sikap_spiritual = row[AlternatifEntity.sikap_spiritual]!!,
                        sikap_sosial = row[AlternatifEntity.sikap_sosial]!!,
                        sakit = row[AlternatifEntity.sakit]!!,
                        izin = row[AlternatifEntity.izin]!!,
                        tanpa_keterangan = row[AlternatifEntity.tanpa_keterangan]!!,
                        total = 0.0
                    )
                }
                // Bobot untuk setiap kriteria
                val bobot = mapOf(
                    "pend_agama" to 0.0277777777777778,
                    "pend_pancasila" to 0.0277777777777778,
                    "bahasa_indo" to 0.0277777777777778,
                    "mtk" to 0.0277777777777778,
                    "sejarah_indo" to 0.0277777777777778,
                    "bahasa_ing" to 0.0277777777777778,
                    "seni_budaya" to 0.0277777777777778,
                    "penjas" to 0.0277777777777778,
                    "prakarya_dan_kwh" to 0.0277777777777778,
                    "biologi" to 0.0277777777777778,
                    "fisika" to 0.0277777777777778,
                    "kimia" to 0.0277777777777778,
                    "sosiologi" to 0.0277777777777778,
                    "ekonomi" to 0.0277777777777778,
                    "ket_pend_agama" to 0.0277777777777778,
                    "ket_pend_pancasila" to 0.0277777777777778,
                    "ket_bahasa_indo" to 0.0277777777777778,
                    "ket_matematika" to 0.0277777777777778,
                    "ket_sejarah_indo" to 0.0277777777777778,
                    "ket_bahasa_ing" to 0.0277777777777778,
                    "ket_seni_budaya" to 0.0277777777777778,
                    "ket_penjas" to 0.0277777777777778,
                    "ket_prakarya_dan_kwh" to 0.0277777777777778,
                    "ket_biologi" to 0.0277777777777778,
                    "ket_fisika" to 0.0277777777777778,
                    "ket_kimia" to 0.0277777777777778,
                    "ket_sosiologi" to 0.0277777777777778,
                    "ket_ekonomi" to 0.0277777777777778,
                    "pramuka" to 0.0277777777777778,
                    "olahraga" to 0.0277777777777778,
                    "pmr" to 0.0277777777777778,
                    "sikap_spiritual" to 0.0277777777777778,
                    "sikap_sosial" to 0.0277777777777778,
                    "sakit" to -0.0277777777777778, // nilai cost untuk sakit
                    "izin" to -0.0277777777777778,// nilai cost untuk izin
                    "tanpa_keterangan" to -0.0277777777777778, // nilai cost untuk absen

                )
                // Nilai maksimum untuk setiap kriteria
                val maxValues = AlternatifReq(
                    pend_agama = maxOf(kriteriaList.maxOf { it.pend_agama }, 0.0000000000000001),
                    pend_pancasila = maxOf(kriteriaList.maxOf { it.pend_pancasila }, 0.0000000000000001),
                    bahasa_indo = maxOf(kriteriaList.maxOf { it.bahasa_indo }, 0.0000000000000001),
                    mtk = maxOf(kriteriaList.maxOf { it.mtk }, 0.0000000000000001),
                    sejarah_indo = maxOf(kriteriaList.maxOf { it.sejarah_indo }, 0.0000000000000001),
                    bahasa_ing = maxOf(kriteriaList.maxOf { it.bahasa_ing }, 0.0000000000000001),
                    seni_budaya = maxOf(kriteriaList.maxOf { it.seni_budaya }, 0.0000000000000001),
                    penjas = maxOf(kriteriaList.maxOf { it.seni_budaya }, 0.0000000000000001),
                    prakarya_dan_kwh = maxOf(kriteriaList.maxOf { it.prakarya_dan_kwh }, 0.0000000000000001),
                    biologi = maxOf(kriteriaList.maxOf { it.biologi }, 0.0000000000000001),
                    fisika = maxOf(kriteriaList.maxOf { it.fisika }, 0.0000000000000001),
                    kimia = maxOf(kriteriaList.maxOf { it.kimia }, 0.0000000000000001),
                    sosiologi = maxOf(kriteriaList.maxOf { it.sosiologi }, 0.0000000000000001),
                    ekonomi = maxOf(kriteriaList.maxOf { it.ekonomi }, 0.0000000000000001),
                    ket_pend_agama = maxOf(kriteriaList.maxOf { it.ket_pend_agama }, 0.0000000000000001),
                    ket_pend_pancasila = maxOf(kriteriaList.maxOf { it.ket_pend_pancasila }, 0.0000000000000001),
                    ket_bahasa_indo = maxOf(kriteriaList.maxOf { it.ket_bahasa_indo }, 0.0000000000000001),
                    ket_matematika = maxOf(kriteriaList.maxOf { it.ket_matematika }, 0.0000000000000001),
                    ket_sejarah_indo = maxOf(kriteriaList.maxOf { it.ket_sejarah_indo }, 0.0000000000000001),
                    ket_bahasa_ing = maxOf(kriteriaList.maxOf { it.ket_bahasa_ing }, 0.0000000000000001),
                    ket_seni_budaya = maxOf(kriteriaList.maxOf { it.ket_seni_budaya }, 0.0000000000000001),
                    ket_penjas = maxOf(kriteriaList.maxOf { it.ket_penjas }, 0.0000000000000001),
                    ket_prakarya_dan_kwh = maxOf(kriteriaList.maxOf { it.ket_prakarya_dan_kwh }, 0.0000000000000001),
                    ket_biologi = maxOf(kriteriaList.maxOf { it.ket_biologi }, 0.0000000000000001),
                    ket_fisika = maxOf(kriteriaList.maxOf { it.ket_fisika }, 0.0000000000000001),
                    ket_kimia = maxOf(kriteriaList.maxOf { it.ket_kimia }, 0.0000000000000001),
                    ket_sosiologi = maxOf(kriteriaList.maxOf { it.ket_sosiologi }, 0.0000000000000001),
                    ket_ekonomi = maxOf(kriteriaList.maxOf { it.ket_ekonomi }, 0.0000000000000001),
                    pramuka = maxOf(kriteriaList.maxOf { it.pramuka }, 0.0000000000000001),
                    olahraga = maxOf(kriteriaList.maxOf { it.olahraga }, 0.0000000000000001),
                    pmr = maxOf(kriteriaList.maxOf { it.pmr }, 0.0000000000000001),
                    sikap_spiritual = maxOf(kriteriaList.maxOf { it.sikap_spiritual }, 0.0000000000000001),
                    sikap_sosial = maxOf(kriteriaList.maxOf { it.sikap_sosial }, 0.0000000000000001),
                    tanpa_keterangan = minOf(
                        kriteriaList.minOf { it.tanpa_keterangan },
                        -0.0000000000000001
                    ), // nilai cost untuk absen
                    izin = minOf(kriteriaList.minOf { it.izin }, -0.0000000000000001),// nilai cost untuk izin
                    sakit = minOf(kriteriaList.minOf { it.sakit }, -0.0000000000000001),// nilai cost untuk sakit
                    total = 0.0,
                    id = 0,
                    nisn = 0,
                    nama = ""
                )
                // Normalisasi nilai setiap kriteria
                val normalizedList = kriteriaList.map { kriteria ->
                    AlternatifReq(
                        kriteria.id,
                        kriteria.nisn,
                        kriteria.nama,
                        kriteria.pend_agama / maxValues.pend_agama,
                        kriteria.pend_pancasila / maxValues.pend_pancasila,
                        kriteria.bahasa_indo / maxValues.bahasa_indo,
                        kriteria.mtk / maxValues.mtk,
                        kriteria.sejarah_indo / maxValues.sejarah_indo,
                        kriteria.bahasa_ing / maxValues.bahasa_ing,
                        kriteria.seni_budaya / maxValues.seni_budaya,
                        kriteria.penjas / maxValues.penjas,
                        kriteria.prakarya_dan_kwh / maxValues.prakarya_dan_kwh,
                        kriteria.biologi / maxValues.biologi,
                        kriteria.fisika / maxValues.fisika,
                        kriteria.kimia / maxValues.kimia,
                        kriteria.sosiologi / maxValues.sosiologi,
                        kriteria.ekonomi / maxValues.ekonomi,
                        kriteria.ket_pend_agama / maxValues.ket_pend_agama,
                        kriteria.ket_pend_pancasila / maxValues.ket_pend_pancasila,
                        kriteria.ket_bahasa_indo / maxValues.ket_bahasa_indo,
                        kriteria.ket_matematika / maxValues.ket_matematika,
                        kriteria.ket_sejarah_indo / maxValues.ket_sejarah_indo,
                        kriteria.ket_bahasa_ing / maxValues.ket_bahasa_ing,
                        kriteria.ket_seni_budaya / maxValues.ket_seni_budaya,
                        kriteria.ket_penjas / maxValues.ket_penjas,
                        kriteria.ket_prakarya_dan_kwh / maxValues.ket_prakarya_dan_kwh,
                        kriteria.ket_biologi / maxValues.ket_biologi,
                        kriteria.ket_fisika / maxValues.ket_fisika,
                        kriteria.ket_kimia / maxValues.ket_kimia,
                        kriteria.ket_sosiologi / maxValues.ket_sosiologi,
                        kriteria.ket_ekonomi / maxValues.ket_ekonomi,
                        kriteria.pramuka / maxValues.pramuka,
                        kriteria.olahraga / maxValues.olahraga,
                        kriteria.pmr / maxValues.pmr,
                        kriteria.sikap_spiritual / maxValues.sikap_spiritual,
                        kriteria.sikap_sosial / maxValues.sikap_sosial,
                        kriteria.sakit / maxValues.sakit,
                        kriteria.izin / maxValues.izin,
                        kriteria.tanpa_keterangan / maxValues.tanpa_keterangan,
                        kriteria.total
                    )
                }
                // Pembobotan nilai setiap kriteria
                val weightedList = normalizedList.map { kriteria ->
                    AlternatifReq(
                        kriteria.id,
                        kriteria.nisn,
                        kriteria.nama,
                        kriteria.pend_agama * bobot["pend_agama"]!!,
                        kriteria.pend_pancasila * bobot["pend_pancasila"]!!,
                        kriteria.bahasa_indo * bobot["bahasa_indo"]!!,
                        kriteria.mtk * bobot["mtk"]!!,
                        kriteria.sejarah_indo * bobot["sejarah_indo"]!!,
                        kriteria.bahasa_ing * bobot["bahasa_ing"]!!,
                        kriteria.seni_budaya * bobot["seni_budaya"]!!,
                        kriteria.penjas * bobot["penjas"]!!,
                        kriteria.prakarya_dan_kwh * bobot["prakarya_dan_kwh"]!!,
                        kriteria.biologi * bobot["biologi"]!!,
                        kriteria.fisika * bobot["fisika"]!!,
                        kriteria.kimia * bobot["kimia"]!!,
                        kriteria.sosiologi * bobot["sosiologi"]!!,
                        kriteria.ekonomi * bobot["ekonomi"]!!,
                        kriteria.pend_agama * bobot["pend_agama"]!!,
                        kriteria.ket_pend_pancasila * bobot["ket_pend_pancasila"]!!,
                        kriteria.ket_bahasa_indo * bobot["ket_bahasa_indo"]!!,
                        kriteria.ket_matematika * bobot["ket_matematika"]!!,
                        kriteria.ket_sejarah_indo * bobot["ket_sejarah_indo"]!!,
                        kriteria.ket_bahasa_ing * bobot["ket_bahasa_ing"]!!,
                        kriteria.ket_seni_budaya * bobot["ket_seni_budaya"]!!,
                        kriteria.ket_penjas * bobot["ket_penjas"]!!,
                        kriteria.ket_prakarya_dan_kwh * bobot["ket_prakarya_dan_kwh"]!!,
                        kriteria.ket_biologi * bobot["ket_biologi"]!!,
                        kriteria.ket_fisika * bobot["ket_fisika"]!!,
                        kriteria.ket_kimia * bobot["ket_kimia"]!!,
                        kriteria.ket_sosiologi * bobot["ket_sosiologi"]!!,
                        kriteria.ket_ekonomi * bobot["ket_ekonomi"]!!,
                        kriteria.pramuka * bobot["pramuka"]!!,
                        kriteria.olahraga * bobot["olahraga"]!!,
                        kriteria.pmr * bobot["pmr"]!!,
                        kriteria.sikap_spiritual * bobot["sikap_spiritual"]!!,
                        kriteria.sikap_sosial * bobot["sikap_sosial"]!!,
                        kriteria.sakit * bobot["sakit"]!!,
                        kriteria.izin * bobot["izin"]!!, // nilai cost untuk izin
                        kriteria.tanpa_keterangan * bobot["tanpa_keterangan"]!!, // nilai cost untuk absen
                        kriteria.total
                    )
                }
                val sawList = weightedList.map { kriteria ->
                    val sum = (bobot["pend_agama"]!! * kriteria.pend_agama) +
                            (bobot["pend_pancasila"]!! * kriteria.pend_pancasila) +
                            (bobot["bahasa_indo"]!! * kriteria.bahasa_indo) +
                            (bobot["mtk"]!! * kriteria.mtk) +
                            (bobot["sejarah_indo"]!! * kriteria.sejarah_indo) +
                            (bobot["bahasa_ing"]!! * kriteria.bahasa_ing) +
                            (bobot["seni_budaya"]!! * kriteria.seni_budaya) +
                            (bobot["penjas"]!! * kriteria.penjas) +
                            (bobot["prakarya_dan_kwh"]!! * kriteria.prakarya_dan_kwh) +
                            (bobot["biologi"]!! * kriteria.biologi) +
                            (bobot["fisika"]!! * kriteria.fisika) +
                            (bobot["pend_pancasila"]!! * kriteria.kimia) +
                            (bobot["sosiologi"]!! * kriteria.sosiologi) +
                            (bobot["ekonomi"]!! * kriteria.ekonomi) +
                            (bobot["ket_pend_agama"]!! * kriteria.ket_pend_agama) +
                            (bobot["ket_pend_pancasila"]!! * kriteria.ket_pend_pancasila) +
                            (bobot["ket_bahasa_indo"]!! * kriteria.ket_bahasa_indo) +
                            (bobot["ket_matematika"]!! * kriteria.ket_matematika) +
                            (bobot["ket_sejarah_indo"]!! * kriteria.ket_sejarah_indo) +
                            (bobot["ket_bahasa_ing"]!! * kriteria.ket_bahasa_ing) +
                            (bobot["ket_seni_budaya"]!! * kriteria.ket_seni_budaya) +
                            (bobot["ket_penjas"]!! * kriteria.ket_penjas) +
                            (bobot["ket_prakarya_dan_kwh"]!! * kriteria.ket_prakarya_dan_kwh) +
                            (bobot["ket_biologi"]!! * kriteria.ket_biologi) +
                            (bobot["ket_fisika"]!! * kriteria.ket_fisika) +
                            (bobot["ket_kimia"]!! * kriteria.ket_kimia) +
                            (bobot["ket_sosiologi"]!! * kriteria.ket_sosiologi) +
                            (bobot["ket_ekonomi"]!! * kriteria.ket_ekonomi) +
                            (bobot["pramuka"]!! * kriteria.pramuka) +
                            (bobot["olahraga"]!! * kriteria.olahraga) +
                            (bobot["pmr"]!! * kriteria.pmr) +
                            (bobot["sikap_spiritual"]!! * kriteria.sikap_spiritual) +
                            (bobot["sikap_sosial"]!! * kriteria.sikap_sosial) +
                            (bobot["sakit"]!! * kriteria.sakit) // nilai cost untuk izin
                    (bobot["izin"]!! * kriteria.izin) + // nilai cost untuk izin
                            (bobot["tanpa_keterangan"]!! * kriteria.tanpa_keterangan) // nilai cost untuk absen


                    kriteria.copy(total = sum)
                }
                val sortedList = sawList.sortedByDescending { it.total }

                val resultList = mutableListOf<Map<String, Any?>>()

                sortedList.forEachIndexed { index, kriteria ->
                    val map = mapOf(
                        "rank" to index + 1,
                        "nama" to kriteria.nama,
                        "nisn" to kriteria.nisn,
                        "total" to kriteria.total
                    )
                    resultList.add(map)
                }

                call.respond(mapOf("result" to resultList))


            }
        }

    }
}






