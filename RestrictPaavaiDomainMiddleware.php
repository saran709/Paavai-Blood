<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Class RestrictPaavaiDomainMiddleware
 * 
 * Strict Domain verification middleware for Paavai BloodConnect Backend API services.
 * Rejects any login or registration request where the input email does not end with '@paavai.edu.in'.
 * This prevents lateral administrative security bypassing and protects database resources from rogue accounts.
 *
 * Designed to scale for 20,000+ campus actors with low-overhead string parsing.
 */
class RestrictPaavaiDomainMiddleware
{
    /**
     * Handle an incoming request.
     *
     * @param  \Illuminate\Http\Request  $request
     * @param  \Closure(\Illuminate\Http\Request): (\Symfony\Component\HttpFoundation\Response)  $next
     * @return \Symfony\Component\HttpFoundation\Response
     */
    public function handle(Request $request, Closure $next): Response
    {
        // 1. Target inputs: Parse 'email' from request payloads during Auth cycles
        if ($request->has('email')) {
            $email = trim(strtolower($request->input('email')));

            // 2. Enforce absolute check: Must end with '@paavai.edu.in' domain suffix
            if (!str_ends_with($email, '@paavai.edu.in')) {
                
                // Track security violations in the backend logs
                \Log::warning('Unauthorized access attempt rejected (Invalid domain):', [
                    'ip_address' => $request->ip(),
                    'email_tried' => $email,
                    'user_agent' => $request->userAgent()
                ]);

                // Return clean, informative JSON response with standard HTTP 403 status code
                return response()->json([
                    'status' => 'error',
                    'error_code' => 'INVALID_CAMPUS_DOMAIN',
                    'message' => 'Restricted access. Only registered @paavai.edu.in institutional email addresses are permitted on this system.'
                ], Response::HTTP_FORBIDDEN);
            }
        } else if ($request->routeIs('auth.register', 'auth.login')) {
            // Error protection: Reject auth cycles that don't provide an email handle
            return response()->json([
                'status' => 'error',
                'message' => 'Bad Request. The institutional email parameter is mandatory for authentication.'
            ], Response::HTTP_BAD_REQUEST);
        }

        return $next($request);
    }
}
