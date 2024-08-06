SELECT VendorID, [250] AS Emp1, [251] AS Emp2, [256] AS Emp3, [257] AS Emp4, [260] AS Emp5  
FROM   
(SELECT PurchaseOrderID, EmployeeID, VendorID  
FROM Purchasing.PurchaseOrderHeader) p  
PIVOT  
(  
COUNT (PurchaseOrderID)  
FOR EmployeeID IN  
( [250], [251], [256], [257], [260] )  
) AS pvt  
ORDER BY pvt.VendorID;  

GO
IF OBJECT_ID ('dbo.EmployeeOne', 'U') IS NOT NULL
DROP TABLE dbo.EmployeeOne;
GO
IF OBJECT_ID ('dbo.EmployeeTwo', 'U') IS NOT NULL
DROP TABLE dbo.EmployeeTwo;
GO
IF OBJECT_ID ('dbo.EmployeeThree', 'U') IS NOT NULL
DROP TABLE dbo.EmployeeThree;
GO

SELECT pp.LastName, pp.FirstName, e.JobTitle 
INTO dbo.EmployeeOne
FROM Person.Person AS pp JOIN HumanResources.Employee AS e
ON e.BusinessEntityID = pp.BusinessEntityID
WHERE LastName = 'Johnson';
GO
SELECT pp.LastName, pp.FirstName, e.JobTitle 
INTO dbo.EmployeeTwo
FROM Person.Person AS pp JOIN HumanResources.Employee AS e
ON e.BusinessEntityID = pp.BusinessEntityID
WHERE LastName = 'Johnson';
GO
SELECT pp.LastName, pp.FirstName, e.JobTitle 
INTO dbo.EmployeeThree
FROM Person.Person AS pp JOIN HumanResources.Employee AS e
ON e.BusinessEntityID = pp.BusinessEntityID
WHERE LastName = 'Johnson';
GO
-- Union ALL
SELECT LastName, FirstName, JobTitle
FROM dbo.EmployeeOne
UNION ALL
SELECT LastName, FirstName ,JobTitle
FROM dbo.EmployeeTwo
UNION ALL
SELECT LastName, FirstName,JobTitle 
FROM dbo.EmployeeThree;
GO

SELECT LastName, FirstName,JobTitle
FROM dbo.EmployeeOne
UNION 
SELECT LastName, FirstName, JobTitle 
FROM dbo.EmployeeTwo
UNION 
SELECT LastName, FirstName, JobTitle 
FROM dbo.EmployeeThree;
GO

SELECT LastName, FirstName,JobTitle 
FROM dbo.EmployeeOne
UNION ALL
(
SELECT LastName, FirstName, JobTitle 
FROM dbo.EmployeeTwo
UNION
SELECT LastName, FirstName, JobTitle 
FROM dbo.EmployeeThree
);
GO


--1) Setup the Common Table Expresions:
with cte707 as (
SELECT  CustomerID, sd.[ProductID], p.Name 
FROM [Sales].[SalesOrderHeader] sh
   INNER JOIN [Sales].[SalesOrderDetail] sd ON sd.SalesOrderID = sh.SalesOrderID
   inner join [Production].[Product] p ON p.ProductID = sd.ProductID
WHERE sd.[ProductID] = 707 
), cte712 as (
 
SELECT  CustomerID, sd.[ProductID], p.Name 
FROM [Sales].[SalesOrderHeader] sh
   INNER JOIN [Sales].[SalesOrderDetail] sd ON sd.SalesOrderID = sh.SalesOrderID
   inner join [Production].[Product] p ON p.ProductID = sd.ProductID
WHERE sd.[ProductID] = 712
)

--2) See the Results:
SELECT a.CustomerID as 'Cust w/ Sport-100 Helmet', 
   b.CustomerID as 'Custs w/ AWC Logo Cap',
   CASE  WHEN b.CustomerID is null THEN 'Only Sport-100 Helmet'
        WHEN a.CustomerID is null THEN 'Only AWC Logo Cap'
        ELSE 'Both - Sport-100 Helmet & AWC Logo Cap'
   END as 'Results'
FROM cte707 a
   FULL OUTER JOIN cte712 b ON a.CustomerID = b.CustomerID;
GO