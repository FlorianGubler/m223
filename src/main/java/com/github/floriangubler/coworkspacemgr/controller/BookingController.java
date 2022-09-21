package com.github.floriangubler.coworkspacemgr.controller;

import com.github.floriangubler.coworkspacemgr.exception.BookingNotFoundException;
import com.github.floriangubler.coworkspacemgr.entity.BookingEntity;
import com.github.floriangubler.coworkspacemgr.service.BookingService;
import com.github.floriangubler.coworkspacemgr.entity.BookingStatus;
import com.github.floriangubler.coworkspacemgr.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import javax.naming.NoPermissionException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Booking", description = "Coworkspace Booking management endpoints")
public class BookingController {

    private final BookingService bookingService;
    private final MemberService memberService;
    private final static String ADMINROLE = "ROLE_ADMIN";

    BookingController(BookingService bookingService, MemberService memberService) {
        this.bookingService = bookingService;
        this.memberService = memberService;
    }

    @Operation(
            summary = "Get bookings",
            description = "Get all bookings (for users anonymised) or only bookings of logged-in User",
            security = {@SecurityRequirement(name = "JWT Auth")}
    )
    @GetMapping("/{onlymy}")
    List<BookingEntity> loadUserBookings(
            @Parameter(description = "Onlymy", required = false)
            @RequestParam(name = "onlymy", required = false)
            Boolean onlymy,
            Authentication authentication) {
        UUID userid = UUID.fromString(authentication.getName());
        Set<String> userroles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        if(onlymy != null && onlymy){
            return bookingService.findUserBookings(userid);
        } else{
            return bookingService.findBookings(!userroles.contains(ADMINROLE), userid);
        }
    }

    @Operation(
            summary = "Delete a booking",
            description = "User delete own bookings, admins delete any booking",
            security = {@SecurityRequirement(name = "JWT Auth")}
    )
    @DeleteMapping("/{bookingid}")
    ResponseEntity<Void> deletebooking(
            @Parameter(description = "BookingID", required = true)
            @RequestParam(name = "bookingid", required = true)
            UUID bookingid,
            Authentication authentication) {try{
            bookingService.delete(bookingid, authentication);
        } catch(BookingNotFoundException e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (NoPermissionException e){
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(
            summary = "Create a booking",
            description = "Create a new Booking (for users with status OPENED, for Admins status ACCEPTED)",
            security = {@SecurityRequirement(name = "JWT Auth")}
    )
    @PostMapping("/")
    BookingEntity createbooking(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Booking", required = true)
            @RequestBody(required = true)
            BookingEntity booking,
            Authentication authentication) {
        booking.setMember(memberService.getMember(booking.getMemberId()));
        if(getRolesSet(authentication).contains(ADMINROLE)){
            booking.setStatus(BookingStatus.APPROVED);
        } else{
            booking.setStatus(BookingStatus.ORDERED);
        }
        return bookingService.create(booking);
    }

    @Operation(
            summary = "Update Booking",
            description = "Update a Booking (Only Admin)",
            security = {@SecurityRequirement(name = "JWT Auth")}
    )
    @PutMapping("/{bookingid}")
    @PreAuthorize("hasRole(ADMINROLE)")
    ResponseEntity<BookingEntity> updatebooking(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Booking Update", required = true)
            @RequestBody(required = true)
            BookingEntity booking,
            @Parameter(description = "BookingID", required = true)
            @RequestParam(name="bookingid", required = true)
            UUID bookingid,
            Authentication authentication) {
        try{
            return new ResponseEntity<>(bookingService.update(booking, bookingid), HttpStatus.OK);
        } catch(BookingNotFoundException e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    //Get Roles as String Set
    private Set<String> getRolesSet(Authentication authentication){
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }
}
