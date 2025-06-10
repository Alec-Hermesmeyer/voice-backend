# RAG System Usage Examples

This document provides practical examples of how to use the Voice-Controlled Backend with RAG capabilities for different client scenarios.

## 🏥 Example 1: Healthcare Client

### Setting up a Healthcare Knowledge Base

```javascript
// Initialize knowledge base for a healthcare client
const healthcareClient = new KnowledgeBaseManager('healthcare-clinic-001');

// Add medical procedures documentation
await healthcareClient.addDocument({
  id: 'procedures-guide',
  content: `
    Patient Check-in Process:
    1. Patient arrives and checks in at reception
    2. Verify insurance information
    3. Update patient demographics
    4. Collect co-payment if applicable
    5. Notify clinical staff of patient arrival
    
    Appointment Scheduling:
    - Use the calendar system to schedule appointments
    - Check provider availability
    - Send confirmation to patient via email/SMS
    - Block time for procedures requiring extended visits
  `,
  source: 'manual',
  metadata: {
    type: 'procedure',
    department: 'administration',
    version: '2.1'
  }
});

// Add FAQ documentation
await healthcareClient.addDocument({
  id: 'patient-faq',
  content: `
    Frequently Asked Questions:
    
    Q: How do I cancel an appointment?
    A: Go to the appointments page, find your appointment, and click the cancel button. You can also call our office at (555) 123-4567.
    
    Q: What insurance do you accept?
    A: We accept most major insurance plans including Blue Cross, Aetna, Cigna, and Medicare.
    
    Q: How do I request prescription refills?
    A: Use the prescription refill form in the patient portal or call our pharmacy line.
  `,
  source: 'faq',
  metadata: {
    type: 'faq',
    audience: 'patients',
    lastUpdated: '2024-01-15'
  }
});
```

### Voice Interactions with Healthcare RAG

```javascript
// Example voice commands that would use the healthcare knowledge base

// Navigation with context
await healthcareClient.processVoiceQuery(
  "How do I schedule a new patient appointment?", 
  "dashboard"
);
// Response: "To schedule a new patient appointment, use the calendar system to check provider availability, then block the appropriate time slot. Make sure to send confirmation to the patient via email or SMS."

// FAQ lookup
await healthcareClient.processVoiceQuery(
  "What insurance plans do you accept?", 
  "patient-registration"
);
// Response: "We accept most major insurance plans including Blue Cross, Aetna, Cigna, and Medicare."

// Procedure guidance
await healthcareClient.processVoiceQuery(
  "Walk me through the patient check-in process", 
  "reception"
);
// Response: "The patient check-in process involves: 1) Patient arrives and checks in at reception, 2) Verify insurance information, 3) Update patient demographics, 4) Collect co-payment if applicable, 5) Notify clinical staff of patient arrival."
```

## 🏦 Example 2: Financial Services Client

### Setting up Financial Knowledge Base

```javascript
const financialClient = new KnowledgeBaseManager('bank-regional-001');

// Upload policy documents
const policyFiles = [
  'loan-approval-process.pdf',
  'account-opening-procedures.pdf',
  'compliance-guidelines.pdf'
];

await financialClient.uploadFiles(policyFiles, 'policy-docs');

// Add real-time data context
await financialClient.addDocument({
  id: 'current-rates',
  content: `
    Current Interest Rates (Updated Daily):
    - Savings Account: 0.5% APY
    - Certificate of Deposit (12 months): 4.2% APY
    - Home Mortgage (30-year fixed): 7.1% APR
    - Personal Loan: 9.5-15.5% APR (based on credit score)
    - Auto Loan: 5.8-8.2% APR
    
    Loan Limits:
    - Personal Loan: $1,000 - $50,000
    - Auto Loan: Up to 110% of vehicle value
    - Home Mortgage: Based on debt-to-income ratio
  `,
  source: 'rate-sheet',
  metadata: {
    type: 'rates',
    updateFrequency: 'daily',
    lastUpdated: new Date().toISOString()
  }
});
```

### Voice Commands for Financial Services

```javascript
// Rate inquiries
await financialClient.processVoiceQuery(
  "What are your current mortgage rates?", 
  "loan-application"
);
// Response: "Our current 30-year fixed home mortgage rate is 7.1% APR. Rates may vary based on your credit score and other factors."

// Process guidance
await financialClient.processVoiceQuery(
  "How do I open a new checking account?", 
  "account-services"
);
// Response: Based on the account-opening-procedures.pdf, provides step-by-step guidance

// Navigation with context
await financialClient.processVoiceQuery(
  "Take me to the loan application form", 
  "main-dashboard"
);
// Response: Navigates to loan application while providing relevant rate information
```

## 🏢 Example 3: Corporate HR Client

### Setting up HR Knowledge Base

```javascript
const hrClient = new KnowledgeBaseManager('corp-hr-001');

// Employee handbook
await hrClient.addDocument({
  id: 'employee-handbook',
  content: `
    Time Off Policies:
    - Vacation Time: 15 days per year (first year), 20 days (2-5 years), 25 days (5+ years)
    - Sick Leave: 10 days per year
    - Personal Days: 3 days per year
    - Holidays: 12 company holidays observed
    
    Requesting Time Off:
    1. Submit request at least 2 weeks in advance
    2. Get manager approval
    3. HR will confirm in the system
    4. Update team calendar
    
    Benefits Enrollment:
    - Open enrollment period: November 1-30
    - New employee enrollment: Within 30 days of start date
    - Qualifying life events allow mid-year changes
  `,
  source: 'handbook',
  metadata: {
    type: 'policy',
    section: 'benefits-timeoff',
    version: '2024.1'
  }
});

// Payroll information
await hrClient.addDocument({
  id: 'payroll-guide',
  content: `
    Payroll Schedule:
    - Pay periods: Bi-weekly (every other Friday)
    - Timesheet deadline: Tuesday at 5 PM
    - Direct deposit: Available on pay date
    - Pay stubs: Available in employee portal
    
    Updating Information:
    - Address changes: Update in employee portal
    - Tax withholding: Submit new W-4 to HR
    - Direct deposit: Provide voided check to HR
    - Emergency contacts: Update in employee portal
  `,
  source: 'payroll-guide',
  metadata: {
    type: 'procedure',
    department: 'payroll'
  }
});
```

### Voice Interactions for HR

```javascript
// Policy inquiries
await hrClient.processVoiceQuery(
  "How many vacation days do I get?", 
  "employee-dashboard"
);
// Response: "Vacation time depends on your length of service: 15 days for your first year, 20 days for 2-5 years, and 25 days for 5+ years of service."

// Process guidance
await hrClient.processVoiceQuery(
  "How do I request time off?", 
  "time-off-portal"
);
// Response: "To request time off: 1) Submit your request at least 2 weeks in advance, 2) Get manager approval, 3) HR will confirm in the system, 4) Update the team calendar."

// Benefits information
await hrClient.processVoiceQuery(
  "When is open enrollment?", 
  "benefits-page"
);
// Response: "Open enrollment period is November 1-30. New employees can enroll within 30 days of their start date, and mid-year changes are allowed for qualifying life events."
```

## 🛒 Example 4: E-commerce Client

### Setting up E-commerce Knowledge Base

```javascript
const ecommerceClient = new KnowledgeBaseManager('shop-online-001');

// Product information
await ecommerceClient.addDocument({
  id: 'product-catalog',
  content: `
    Featured Products:
    
    Wireless Bluetooth Headphones - Model WH-1000XM4
    - Price: $349.99
    - Features: Noise canceling, 30-hour battery, touch controls
    - Availability: In stock
    - Shipping: Free 2-day shipping
    
    Smart Fitness Watch - Model SW-500
    - Price: $199.99
    - Features: Heart rate monitor, GPS, waterproof, 7-day battery
    - Availability: Limited stock (12 units)
    - Shipping: Standard shipping $5.99
    
    4K Streaming Device - Model SD-4K
    - Price: $59.99
    - Features: 4K HDR, voice remote, Wi-Fi 6
    - Availability: In stock
    - Shipping: Free shipping on orders over $50
  `,
  source: 'catalog',
  metadata: {
    type: 'products',
    category: 'electronics',
    lastUpdated: new Date().toISOString()
  }
});

// Shipping and return policies
await ecommerceClient.addDocument({
  id: 'shipping-returns',
  content: `
    Shipping Policies:
    - Free shipping on orders over $50
    - Standard shipping: 5-7 business days ($5.99)
    - Express shipping: 2-3 business days ($12.99)
    - Next day shipping: 1 business day ($24.99)
    
    Return Policy:
    - 30-day return window
    - Items must be in original packaging
    - Electronics require all accessories
    - Free return shipping for defective items
    - Return shipping fee: $7.99 for non-defective returns
    
    Customer Service:
    - Live chat: Available 9 AM - 9 PM EST
    - Phone: 1-800-SHOP-NOW
    - Email: support@shop-online.com
    - Response time: Within 24 hours
  `,
  source: 'policies',
  metadata: {
    type: 'policy',
    section: 'shipping-returns'
  }
});
```

### Voice Shopping Experience

```javascript
// Product search
await ecommerceClient.processVoiceQuery(
  "Show me wireless headphones", 
  "product-search"
);
// Response: "I found the Wireless Bluetooth Headphones - Model WH-1000XM4 for $349.99. Features include noise canceling, 30-hour battery, and touch controls. It's currently in stock with free 2-day shipping."

// Policy inquiries
await ecommerceClient.processVoiceQuery(
  "What's your return policy?", 
  "checkout"
);
// Response: "We offer a 30-day return window. Items must be in original packaging, and electronics require all accessories. Free return shipping for defective items, or $7.99 for non-defective returns."

// Navigation with recommendations
await ecommerceClient.processVoiceQuery(
  "Take me to electronics", 
  "homepage"
);
// Response: Navigates to electronics category while mentioning featured products
```

## 🏗 Implementation Pattern

### Common Setup Pattern

```javascript
class ClientRAGManager {
  constructor(clientId, domain) {
    this.clientId = clientId;
    this.domain = domain;
    this.knowledgeBase = new KnowledgeBaseManager(clientId);
  }
  
  async initializeWithDocuments(documents) {
    const processedDocs = documents.map(doc => ({
      ...doc,
      metadata: {
        ...doc.metadata,
        domain: this.domain,
        clientId: this.clientId,
        importedAt: new Date().toISOString()
      }
    }));
    
    return await this.knowledgeBase.initializeKnowledgeBase(processedDocs);
  }
  
  async handleVoiceCommand(command, context) {
    try {
      // First try RAG-enhanced processing
      const ragResponse = await this.knowledgeBase.processVoiceQuery(command, context);
      
      if (ragResponse.success && ragResponse.relevantDocumentsCount > 0) {
        return {
          type: 'rag_response',
          response: ragResponse.response,
          confidence: 'high',
          source: 'knowledge_base'
        };
      }
      
      // Fallback to standard voice processing
      return await this.processStandardVoiceCommand(command, context);
      
    } catch (error) {
      console.error('RAG processing failed:', error);
      return await this.processStandardVoiceCommand(command, context);
    }
  }
  
  async processStandardVoiceCommand(command, context) {
    // Standard voice command processing without RAG
    const response = await fetch('/api/voice/command', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        transcript: command,
        currentContext: context,
        clientId: this.clientId
      })
    });
    
    return await response.json();
  }
}
```

### React Integration Example

```typescript
// hooks/useClientRAG.ts
import { useState, useEffect, useCallback } from 'react';

interface UseClientRAGProps {
  clientId: string;
  domain: string;
  initialDocuments?: any[];
}

export const useClientRAG = ({ clientId, domain, initialDocuments }: UseClientRAGProps) => {
  const [ragManager, setRagManager] = useState<ClientRAGManager | null>(null);
  const [isInitialized, setIsInitialized] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  
  useEffect(() => {
    const manager = new ClientRAGManager(clientId, domain);
    setRagManager(manager);
    
    if (initialDocuments && initialDocuments.length > 0) {
      initializeKnowledgeBase(manager, initialDocuments);
    } else {
      setIsInitialized(true);
    }
  }, [clientId, domain]);
  
  const initializeKnowledgeBase = async (manager: ClientRAGManager, documents: any[]) => {
    setIsLoading(true);
    try {
      await manager.initializeWithDocuments(documents);
      setIsInitialized(true);
    } catch (error) {
      console.error('Failed to initialize knowledge base:', error);
    } finally {
      setIsLoading(false);
    }
  };
  
  const processVoiceCommand = useCallback(async (command: string, context: string) => {
    if (!ragManager || !isInitialized) {
      throw new Error('RAG manager not initialized');
    }
    
    return await ragManager.handleVoiceCommand(command, context);
  }, [ragManager, isInitialized]);
  
  const addDocument = useCallback(async (document: any) => {
    if (!ragManager) return;
    
    return await ragManager.knowledgeBase.addDocument(document);
  }, [ragManager]);
  
  const getStats = useCallback(async () => {
    if (!ragManager) return null;
    
    return await ragManager.knowledgeBase.getStats();
  }, [ragManager]);
  
  return {
    isInitialized,
    isLoading,
    processVoiceCommand,
    addDocument,
    getStats
  };
};
```

## 📊 Performance Monitoring

### Tracking RAG Effectiveness

```javascript
class RAGMetrics {
  constructor(clientId) {
    this.clientId = clientId;
    this.metrics = {
      totalQueries: 0,
      ragHits: 0,
      ragMisses: 0,
      averageRelevanceScore: 0,
      responseTime: []
    };
  }
  
  async trackQuery(query, response) {
    this.metrics.totalQueries++;
    
    if (response.type === 'rag_response') {
      this.metrics.ragHits++;
      this.updateRelevanceScore(response.relevanceScore);
    } else {
      this.metrics.ragMisses++;
    }
    
    this.metrics.responseTime.push(response.processingTime);
    
    // Send metrics to analytics service
    await this.sendMetrics();
  }
  
  getEffectivenessRate() {
    return this.metrics.ragHits / this.metrics.totalQueries;
  }
  
  getAverageResponseTime() {
    const times = this.metrics.responseTime;
    return times.reduce((sum, time) => sum + time, 0) / times.length;
  }
}
```

This comprehensive example guide shows how the RAG system can be tailored for different industries and use cases, providing intelligent, context-aware voice interactions that leverage client-specific knowledge bases. 