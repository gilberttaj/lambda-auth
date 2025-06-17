# Comprehensive AWS Console Prerequisite Setup Guide for Private LambdaAuth

This guide provides detailed instructions for manually configuring the necessary AWS resources in the AWS Management Console. These resources are prerequisites for deploying the SAM (Serverless Application Model) template for the LambdaAuth project, which involves a private API Gateway and a Lambda function within a VPC.

**Target Audience:** Users who will be setting up the AWS infrastructure manually.
**Goal:** To have all networking and security components in place before SAM deployment.

---

## I. Foundational VPC Setup

Your Lambda function and private API Gateway will reside within a Virtual Private Cloud (VPC).

### 1. Virtual Private Cloud (VPC)
*   **Purpose:** Provides an isolated network environment in AWS.
*   **Action:**
    1.  Navigate to the **VPC Dashboard** in the AWS Console.
    2.  Click "Create VPC."
    3.  Select "VPC only" or "VPC and more" (if you prefer the wizard, but ensure you understand the components it creates).
    4.  **Name tag:** (e.g., `LambdaAuth-VPC`).
    5.  **IPv4 CIDR block:** Choose a private IP range (e.g., `10.0.0.0/16`). This range should not overlap with other connected networks.
    6.  Leave other settings as default unless you have specific needs (e.g., IPv6, Tenancy).
    7.  Click "Create VPC."
*   **Collect:**
    *   **VPC ID:** (e.g., `vpc-0123abcdxxxxxxxx`) - Note this down.

### 2. Subnets
Subnets are segments of your VPC's IP address range where you can place groups of isolated resources.

#### a. Private Subnets (At least 2 for High Availability)
*   **Purpose:** Host your Lambda function's network interfaces. These subnets will not have direct internet access but will use a NAT Gateway for outbound connections.
*   **Action (Repeat for each private subnet):**
    1.  In the VPC Dashboard, go to "Subnets" > "Create subnet."
    2.  **VPC ID:** Select your `LambdaAuth-VPC`.
    3.  **Subnet name:** (e.g., `LambdaAuth-PrivateSubnet-AZ1`, `LambdaAuth-PrivateSubnet-AZ2`).
    4.  **Availability Zone:** Choose a different AZ for each private subnet for high availability.
    5.  **IPv4 CIDR block:** Assign a unique CIDR block from your VPC's range (e.g., `10.0.1.0/24`, `10.0.2.0/24`).
    6.  Click "Create subnet."
*   **Collect:**
    *   **Private Subnet IDs:** (e.g., `subnet-0123...`, `subnet-0456...`) - These are critical for the SAM template.

#### b. Public Subnets (At least 1, preferably 2 for NAT Gateway HA)
*   **Purpose:** Host resources that need direct internet access, like your NAT Gateway.
*   **Action (Repeat for each public subnet):**
    1.  In the VPC Dashboard, go to "Subnets" > "Create subnet."
    2.  **VPC ID:** Select your `LambdaAuth-VPC`.
    3.  **Subnet name:** (e.g., `LambdaAuth-PublicSubnet-AZ1`, `LambdaAuth-PublicSubnet-AZ2`).
    4.  **Availability Zone:** Choose an AZ (ideally matching one of your private subnets if planning zonal NAT Gateways).
    5.  **IPv4 CIDR block:** Assign a unique CIDR block (e.g., `10.0.100.0/24`, `10.0.101.0/24`).
    6.  Click "Create subnet."
*   **Collect:**
    *   **Public Subnet IDs:** (For reference during IGW and NGW setup).

### 3. Internet Gateway (IGW)
*   **Purpose:** Enables communication between your VPC and the internet. Required for public subnets.
*   **Action:**
    1.  In the VPC Dashboard, go to "Internet Gateways" > "Create internet gateway."
    2.  **Name tag:** (e.g., `LambdaAuth-IGW`).
    3.  Click "Create internet gateway."
    4.  Select the newly created IGW, then "Actions" > "Attach to VPC."
    5.  Select your `LambdaAuth-VPC` and click "Attach internet gateway."
*   **Collect:**
    *   **Internet Gateway ID:** (For reference).

### 4. Elastic IP Address (EIP) & NAT Gateway (NGW)
*   **Purpose:** NAT Gateway allows instances in private subnets to initiate outbound IPv4 traffic to the internet or other AWS services, while preventing instances from receiving inbound traffic initiated from the internet. An EIP provides a static public IP for the NGW.
*   **Action:**
    1.  **Allocate Elastic IP:**
        *   In the VPC Dashboard, go to "Elastic IPs" > "Allocate Elastic IP address."
        *   Network Border Group: Choose your region.
        *   Click "Allocate."
        *   **Collect EIP Allocation ID and Public IP.**
    2.  **Create NAT Gateway:**
        *   In the VPC Dashboard, go to "NAT Gateways" > "Create NAT gateway."
        *   **Name:** (e.g., `LambdaAuth-NGW-AZ1`).
        *   **Subnet:** Select one of your **public subnets**.
        *   **Connectivity type:** Public.
        *   **Elastic IP allocation ID:** Select the EIP you just allocated.
        *   Click "Create NAT gateway." (Provisioning can take a few minutes).
    *   *(Optional for HA: Repeat for a second public subnet in a different AZ with a new EIP).*
*   **Collect:**
    *   **NAT Gateway ID(s):** (e.g., `nat-0abcdefg...`) - For private route table configuration.

### 5. Route Tables
Route tables determine where network traffic from your subnets is directed.

#### a. Public Route Table
*   **Purpose:** Routes traffic from public subnets to the Internet Gateway.
*   **Action:**
    1.  In the VPC Dashboard, go to "Route Tables" > "Create route table."
    2.  **Name:** (e.g., `LambdaAuth-Public-RT`).
    3.  **VPC:** Select your `LambdaAuth-VPC`.
    4.  Click "Create route table."
    5.  Select the new Public Route Table.
    6.  Go to the "Routes" tab > "Edit routes."
    7.  Click "Add route":
        *   **Destination:** `0.0.0.0/0`
        *   **Target:** Select "Internet Gateway" and then your `LambdaAuth-IGW`.
    8.  Click "Save changes."
    9.  Go to the "Subnet associations" tab > "Edit subnet associations."
    10. Select your **public subnets** and click "Save associations."

#### b. Private Route Table(s)
*   **Purpose:** Routes outbound internet traffic from private subnets through the NAT Gateway.
*   **Action:**
    1.  In the VPC Dashboard, go to "Route Tables" > "Create route table."
    2.  **Name:** (e.g., `LambdaAuth-Private-RT`).
    3.  **VPC:** Select your `LambdaAuth-VPC`.
    4.  Click "Create route table."
    5.  Select the new Private Route Table.
    6.  Go to the "Routes" tab > "Edit routes."
    7.  Click "Add route":
        *   **Destination:** `0.0.0.0/0`
        *   **Target:** Select "NAT Gateway" and then your `LambdaAuth-NGW`.
    8.  Click "Save changes."
    9.  Go to the "Subnet associations" tab > "Edit subnet associations."
    10. Select your **private subnets** and click "Save associations."

---

## II. Security Configuration

### 6. Security Groups (SGs)
Security Groups act as virtual firewalls for your resources, controlling inbound and outbound traffic.

#### a. Lambda Security Group
*   **Purpose:** Controls traffic for the Lambda function's network interfaces.
*   **Action:**
    1.  In the VPC Dashboard, go to "Security Groups" (under Security) > "Create security group."
    2.  **Security group name:** (e.g., `LambdaAuth-Lambda-SG`).
    3.  **Description:** (e.g., "SG for LambdaAuth function").
    4.  **VPC:** Select your `LambdaAuth-VPC`.
    5.  **Inbound rules:**
        *   Typically, no inbound rules are strictly needed *from external sources* for Lambda invocation via API Gateway if using VPC endpoints correctly. However, if the API Gateway VPC Endpoint uses a *different* SG, you might add a rule here allowing HTTPS from the API Gateway VPC Endpoint's SG. For simplicity, if both use this SG or if the API Gateway endpoint allows all outbound to the Lambda's private subnet IPs, this can be minimal.
        *   *Consideration:* Some setups might allow all traffic from the API Gateway VPC Endpoint Security Group if they are separate.
    6.  **Outbound rules:**
        *   Click "Add rule."
            *   **Type:** HTTPS
            *   **Protocol:** TCP
            *   **Port range:** 443
            *   **Destination:** `0.0.0.0/0` (Allows Lambda to reach Cognito JWKS URL, other AWS services via public endpoints if not using VPC endpoints, etc.)
        *   Click "Create security group."
*   **Collect:**
    *   **Lambda Security Group ID:** (e.g., `sg-0123abcd...`) - Critical for SAM template.

#### b. API Gateway VPC Endpoint Security Group
*   **Purpose:** Controls traffic to the API Gateway VPC Endpoint.
*   **Action:**
    1.  In the VPC Dashboard, go to "Security Groups" > "Create security group."
    2.  **Security group name:** (e.g., `LambdaAuth-VPCE-SG`).
    3.  **Description:** (e.g., "SG for API Gateway VPC Endpoint").
    4.  **VPC:** Select your `LambdaAuth-VPC`.
    5.  **Inbound rules:**
        *   Click "Add rule."
            *   **Type:** HTTPS
            *   **Protocol:** TCP
            *   **Port range:** 443
            *   **Source:** Your client's IP range (e.g., your corporate network CIDR, VPN client IP pool, or `0.0.0.0/0` if access is controlled by VPN connectivity itself. Be as specific as possible for better security).
    6.  **Outbound rules:**
        *   Default (Allow all outbound) is often sufficient.
        *   *More restrictive option:* Allow HTTPS (TCP 443) to the CIDR ranges of your **private subnets** where the Lambda function will run, or specifically to the `LambdaAuth-Lambda-SG`.
    7.  Click "Create security group."
*   **Collect:**
    *   **API Gateway VPC Endpoint Security Group ID:** (For reference and potential use in Lambda SG rules).

---

## III. VPC Endpoints (for Private Connectivity to AWS Services)

VPC Endpoints enable private connections between your VPC and supported AWS services without requiring an internet gateway, NAT device, VPN connection, or AWS Direct Connect connection.

### 7. API Gateway VPC Endpoint (Interface Endpoint for `execute-api`)
*   **Purpose:** Makes your API Gateway private, accessible only from within your VPC or connected networks (like VPN).
*   **Action:**
    1.  In the VPC Dashboard, go to "Endpoints" > "Create endpoint."
    2.  **Name tag:** (e.g., `LambdaAuth-APIGW-VPCE`).
    3.  **Service category:** "AWS services."
    4.  **Services:** Search for `execute-api` and select the service name (e.g., `com.amazonaws.<region>.execute-api`).
    5.  **VPC:** Select your `LambdaAuth-VPC`.
    6.  **Subnets:** Select the **private subnets** where you want the endpoint's network interfaces to be created. These should be accessible by your clients (e.g., via VPN routing).
    7.  **IP address type:** IPv4.
    8.  **Enable Private DNS Name:** **Check this box.** This allows you to use the standard public DNS hostname of the API Gateway, which will resolve to private IPs within your VPC.
    9.  **Security group:** Select the `LambdaAuth-VPCE-SG` created earlier.
    10. **Policy:** Full Access (default) is usually fine unless you need to restrict which APIs can be accessed through this endpoint.
    11. Click "Create endpoint." (Provisioning can take a few minutes).
*   **Collect:**
    *   **API Gateway VPC Endpoint ID:** (e.g., `vpce-0abcdefg...`) - Critical for SAM template.

### 8. S3 Gateway Endpoint (Highly Recommended)
*   **Purpose:** Allows resources in your private subnets (like Lambda) to access S3 privately without going through the NAT Gateway. SAM uses S3 for deployment packages.
*   **Action:**
    1.  In the VPC Dashboard, go to "Endpoints" > "Create endpoint."
    2.  **Name tag:** (e.g., `LambdaAuth-S3-VPCE`).
    3.  **Service category:** "AWS services."
    4.  **Services:** Search for `s3` and select the service with type "Gateway" (e.g., `com.amazonaws.<region>.s3`).
    5.  **VPC:** Select your `LambdaAuth-VPC`.
    6.  **Route tables:** Select the **route table(s) associated with your private subnets** (e.g., `LambdaAuth-Private-RT`). The endpoint will add routes to these tables.
    7.  **Policy:** Full Access (default).
    8.  Click "Create endpoint."

### 9. CloudWatch Logs Interface Endpoint (Optional but Recommended)
*   **Purpose:** Allows Lambda to send logs to CloudWatch Logs privately.
*   **Action:**
    1.  In the VPC Dashboard, go to "Endpoints" > "Create endpoint."
    2.  **Name tag:** (e.g., `LambdaAuth-Logs-VPCE`).
    3.  **Service:** Search for `logs` (e.g., `com.amazonaws.<region>.logs`, type "Interface").
    4.  **VPC:** Select your `LambdaAuth-VPC`.
    5.  **Subnets:** Select your **private subnets**.
    6.  **Enable Private DNS Name:** Check.
    7.  **Security group:** Select your `LambdaAuth-Lambda-SG` (as Lambda will be initiating traffic to this endpoint) or a dedicated SG allowing HTTPS from Lambda.
    8.  Click "Create endpoint."

---

## IV. IAM (Identity and Access Management)
*   **Note:** The SAM template will create an IAM Role for the Lambda function. This role typically includes:
    *   `AWSLambdaBasicExecutionRole`: For CloudWatch logging.
    *   `AWSLambdaVPCAccessExecutionRole`: For managing network interfaces in your VPC.
*   **Action:** No manual IAM role creation is needed *before* SAM deployment for the Lambda function itself, unless you have specific organizational policies. You will, however, need IAM permissions for your user/role deploying the SAM template (e.g., permissions to create Lambdas, API Gateways, IAM roles, etc.).

---

## V. AWS Cognito User Pool & Google IdP
*   These are configured separately. Refer to:
    *   [cognitoConfigurationGuide.md](cci:7://file:///c:/Users/Asus/OneDrive/Desktop/GREAMORB/LambdaAuth/cognitoConfigurationGuide.md:0:0-0:0)
    *   [googleCloudInstruction.md](cci:7://file:///c:/Users/Asus/OneDrive/Desktop/GREAMORB/LambdaAuth/googleCloudInstruction.md:0:0-0:0)

---

**Checklist & Collection Summary for SAM Deployment:**

Before running `sam deploy --guided`, ensure you have the following IDs and information:

*   [ ] **Private Subnet IDs (at least two):** `subnet-_________________`, `subnet-_________________`
*   [ ] **Lambda Security Group ID:** `sg-_________________`
*   [ ] **API Gateway VPC Endpoint ID:** `vpce-_________________`
*   [ ] **Allowed Email Domains (comma-separated string):** `___________________________________`

This detailed guide should help ensure all AWS prerequisites are correctly configured in the console.