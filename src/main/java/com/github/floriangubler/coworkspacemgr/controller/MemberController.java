package com.github.floriangubler.coworkspacemgr.controller;

import com.github.floriangubler.coworkspacemgr.entity.MemberEntity;
import com.github.floriangubler.coworkspacemgr.entity.MemberDTO;
import com.github.floriangubler.coworkspacemgr.exception.UserAlreadyExistsException;
import com.github.floriangubler.coworkspacemgr.service.MemberService;
import com.github.floriangubler.coworkspacemgr.exception.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/members")
@Tag(name = "Members", description = "Coworkspace Members management endpoints")
public class MemberController {

    private final MemberService memberService;

    MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @Operation(
            summary = "Get Members",
            description = "Get all Members (Only Admin)",
            security = {@SecurityRequirement(name = "JWT Auth")}
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    List<MemberEntity> loadUserBookings() {
        return memberService.getMembers();
    }

    @Operation(
            summary = "Update a Member",
            description = "Admin updates a Member",
            security = {@SecurityRequirement(name = "JWT Auth")}
    )
    @PutMapping("/{memberid}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> updatemember (
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Member Update", required = true)
            @RequestBody(required = true)
            MemberDTO memberDTO,
            @Parameter(description = "MemberID", required = true)
            @PathVariable(name="memberid", required = true)
            UUID memberid) {
        try{
            String passwordHash = BCrypt.hashpw(memberDTO.getPassword(), BCrypt.gensalt());
            memberService.update(new MemberEntity(UUID.randomUUID(), memberDTO.getEmail(), memberDTO.getFirstname(), memberDTO.getLastname(), passwordHash, false), memberid);
        } catch(UserNotFoundException e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (UserAlreadyExistsException e){
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(
            summary = "Delete a Member",
            description = "Delete a Member (Only Admin)",
            security = {@SecurityRequirement(name = "JWT Auth")}
    )
    @DeleteMapping("/{memberid}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deletemember(
            @Parameter(description = "MemberID", required = true)
            @PathVariable(name = "memberid", required = true)
            UUID memberid) {
            try{
                memberService.delete(memberid);
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } catch(UserNotFoundException e){
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
    }
}
