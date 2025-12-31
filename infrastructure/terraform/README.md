# Terraform AWS Infrastructure

Deployment automatico infrastruttura AWS per e-commerce microservices.

## Prerequisiti

- [x] Account AWS creato
- [x] AWS CLI installato e configurato (`aws configure`)
- [x] Terraform installato (`terraform version`)
- [x] Git Bash o PowerShell

## Setup

### 1. Genera SSH Key
```bash
cd infrastructure/terraform
ssh-keygen -t rsa -b 4096 -f k3s-key -C "aws-k3s"
# Premi ENTER per no passphrase (o inserisci se vuoi)
```

**Genera:**
- `k3s-key` (privata - NON committare)
- `k3s-key.pub` (pubblica - usata da Terraform)

### 2. Ottieni Tuo IP Pubblico
```bash
curl ifconfig.me
# Output: 93.45.123.78
```

### 3. Crea terraform.tfvars
```bash
cp terraform.tfvars.example terraform.tfvars
```

**Modifica `terraform.tfvars`:**
```hcl
my_ip = "93.45.123.78/32"  # SOSTITUISCI CON TUO IP!
```

### 4. Verifica .gitignore

**File root progetto `.gitignore`:**
```
infrastructure/terraform/.terraform/
infrastructure/terraform/*.tfstate
infrastructure/terraform/*.tfstate.backup
infrastructure/terraform/terraform.tfvars
infrastructure/terraform/k3s-key
infrastructure/terraform/k3s-key.pub
```

## Deploy

### 1. Initialize Terraform
```bash
terraform init
```

**Output:**
```
Initializing the backend...
Initializing provider plugins...
- Installing hashicorp/aws v5.x.x...
Terraform has been successfully initialized!
```

### 2. Plan (Preview)
```bash
terraform plan
```

**Mostra cosa verrà creato:**
- VPC + Subnets
- Security Groups
- EC2 instance
- Elastic IP
- IAM roles

### 3. Apply (Deploy)
```bash
terraform apply
```

**Conferma:** Digita `yes`

**Tempo:** ~5 minuti

**Output finale:**
```
Apply complete! Resources: 15 added, 0 changed, 0 destroyed.

Outputs:

ec2_public_ip = "3.251.45.89"
ssh_command = "ssh -i k3s-key ubuntu@3.251.45.89"
```

## Verifica Deploy

### 1. SSH su EC2
```bash
ssh -i k3s-key ubuntu@3.251.45.89
```

### 2. Verifica K3s
```bash
# Su EC2
k3s kubectl get nodes

# Output:
# NAME            STATUS   ROLES    AGE   VERSION
# ecommerce-k3s   Ready    master   5m    v1.28.x+k3s1
```

### 3. Copy Kubeconfig Locale
```bash
# Dal tuo laptop
scp -i k3s-key ubuntu@3.251.45.89:/home/ubuntu/.kube/config ~/.kube/config-aws

# Set env
export KUBECONFIG=~/.kube/config-aws

# Test
kubectl get nodes
```

## Costi Stimati

### Budget Mode (Default)
- EC2 t3.small: $15.20/mese
- EBS 30GB: $2.40/mese (free tier anno 1)
- Elastic IP: $0 (se EC2 running)
- **TOTALE: ~$15-17/mese** (anno 1 con free tier)

### Free Tier (t3.micro)
Cambia in `terraform.tfvars`:
```hcl
instance_type = "t3.micro"
```
- **TOTALE: $0/mese** (anno 1)
- ⚠️ 1GB RAM (limite per 4 servizi + DB)

## Stop/Start EC2

### Stop (Risparmi Instance Hours)
```bash
aws ec2 stop-instances --instance-ids $(terraform output -raw ec2_instance_id)
```

### Start
```bash
aws ec2 start-instances --instance-ids $(terraform output -raw ec2_instance_id)
```

## Destroy (Delete Tutto)
```bash
terraform destroy
# Conferma: yes
```

**⚠️ Cancella TUTTO (EC2, VPC, EIP)!**

**Billing stop immediato** ✅

## Troubleshooting

### Error: InvalidClientTokenId
```bash
aws sts get-caller-identity
# Verifica credenziali AWS CLI
```

### SSH Permission Denied
```bash
chmod 400 k3s-key
ssh -i k3s-key ubuntu@<IP>
```

### K3s Not Ready
```bash
# Su EC2
sudo systemctl status k3s
sudo journalctl -u k3s -f
```

### User Data Log
```bash
# Su EC2
cat /var/log/user-data-complete.log
sudo cat /var/log/cloud-init-output.log
```

## Next Steps

1. Deploy microservizi K8s
2. Setup ECR (Docker registry)
3. CI/CD integration
4. Monitoring CloudWatch
5. (Optional) Add ALB + RDS